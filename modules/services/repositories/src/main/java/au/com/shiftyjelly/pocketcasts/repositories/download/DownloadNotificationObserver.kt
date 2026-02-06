package au.com.shiftyjelly.pocketcasts.repositories.download

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.deeplink.DownloadsDeepLink
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.repositories.R
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationOpenReceiverActivity
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import au.com.shiftyjelly.pocketcasts.utils.Util
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class DownloadNotificationObserver @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val notificationHelper: NotificationHelper,
    private val downloadProgressCache: DownloadProgressCache,
    private val clock: Clock,
) {
    private val manager = requireNotNull(context.getSystemService<NotificationManager>())

    fun observeNotificationUpdates(
        episode: BaseEpisode,
        onCreateNotification: (Int, Notification) -> Unit,
    ): Job {
        val now = clock.instant()
        val notificationId = episode.notificationId()

        val job = scope.launch {
            var lastNotifiedProgress: DownloadProgress? = null
            downloadProgressCache.progressFlow(episode.uuid).collect { downloadProgress ->
                if (downloadProgress == null) {
                    return@collect
                }
                if (shouldUpdateNotification(current = lastNotifiedProgress, new = downloadProgress)) {
                    val notification = createEpisodeNotification(episode, now, downloadProgress)
                    withContext(Dispatchers.Main) { manager.notify(notificationId, notification) }
                    if (lastNotifiedProgress == null) {
                        onCreateNotification(notificationId, notification)
                    }
                    lastNotifiedProgress = downloadProgress
                }
            }
        }
        job.invokeOnCompletion {
            scope.launch(Dispatchers.Main) { manager.cancel(notificationId) }
        }
        return job
    }

    private fun shouldUpdateNotification(current: DownloadProgress?, new: DownloadProgress): Boolean {
        val currentPercentage = current?.percentage
        val newPercentage = new.percentage
        return current == null || // First event
            (newPercentage == 100 && currentPercentage != 100) || // Completion
            (currentPercentage == null && newPercentage != null) || // First known percentage
            (abs((newPercentage ?: 0) - (currentPercentage ?: 0)) >= 5) // Change by at least 5%
    }

    private fun createEpisodeNotification(
        episode: BaseEpisode,
        startedAt: Instant,
        progress: DownloadProgress,
    ): Notification {
        return notificationHelper.downloadChannelBuilder()
            .setSmallIcon(IR.drawable.notification_download)
            .setColor(ContextCompat.getColor(context, R.color.notification_color))
            .setContentTitle(context.getString(LR.string.downloading, episode.title))
            .setContentIntent(downloadsPendingIntent())
            .setProgress(100, progress.percentage ?: 0, progress.percentage == null)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setWhen(startedAt.toEpochMilli())
            .setShowWhen(false)
            .setSortKey("%013d_%s".format(startedAt.toEpochMilli(), episode.uuid))
            .build()
    }

    private fun downloadsPendingIntent(): PendingIntent {
        val intent = DownloadsDeepLink.toIntent(context)
        return if (Util.getAppPlatform(context) == AppPlatform.Phone) {
            PendingIntent.getActivity(
                context,
                0,
                NotificationOpenReceiverActivity.toDeeplinkIntentRelay(context, intent),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        } else {
            PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}

// NotificationManager allows posting multiple notifications with the same ID
// as long as they are distinguished by different tags. We could use the episode
// UUID as the tag; however, WorkManager's ForegroundInfo does not support tags.
// Since a foreground notification is required to keep the Worker alive, we must
// generate a unique notification ID per episode instead.
//
// This assumes low collision within UUIDs and available Settings.NotificationId values.
private fun BaseEpisode.notificationId(): Int {
    val uuid = UUID.nameUUIDFromBytes(uuid.toByteArray())
    return (uuid.mostSignificantBits xor uuid.leastSignificantBits).toInt() and 0x7fffffff
}
