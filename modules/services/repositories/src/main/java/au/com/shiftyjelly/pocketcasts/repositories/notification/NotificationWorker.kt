package au.com.shiftyjelly.pocketcasts.repositories.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.R
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationSchedulerImpl.Companion.DOWNLOADED_EPISODES
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationSchedulerImpl.Companion.SUBCATEGORY
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import au.com.shiftyjelly.pocketcasts.images.R as IR

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settings: Settings,
    private val notificationHelper: NotificationHelper,
    private val notificationManager: NotificationManager,
    private val podcastManager: PodcastManager,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val subcategory = inputData.getString(SUBCATEGORY) ?: return Result.failure()

        val type = NotificationType.fromSubCategory(subcategory) ?: return Result.failure()

        if (!type.isSettingsToggleOn(settings)) {
            return Result.failure()
        }

        if (notificationManager.hasUserInteractedWithFeature(type) && shouldSchedule(type)) {
            return Result.failure()
        }

        val notification = getNotificationBuilder(type).build()

        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED && FeatureFlag.isEnabled(Feature.NOTIFICATIONS_REVAMP)) {
            NotificationManagerCompat.from(applicationContext).notify(type.notificationId, notification)
            notificationManager.updateNotificationSent(type)
        }

        return Result.success()
    }

    private fun shouldSchedule(type: NotificationType): Boolean {
        return when (type) {
            is NewFeaturesAndTipsNotificationType.SmartFolders -> podcastManager.getSubscribedPodcastUuidsRxSingle().blockingGet().isNotEmpty()
            else -> true
        }
    }

    private fun getNotificationBuilder(type: NotificationType): NotificationCompat.Builder {
        val downloadedEpisodes = inputData.getInt(DOWNLOADED_EPISODES, 0)

        val builder = when (type) {
            is TrendingAndRecommendationsNotificationType -> {
                notificationHelper.trendingAndRecommendationsChannelBuilder()
            }

            is NewFeaturesAndTipsNotificationType -> {
                notificationHelper.featuresAndTipsChannelBuilder()
            }

            else -> notificationHelper.dailyRemindersChannelBuilder()
        }

        return builder
            .setSmallIcon(IR.drawable.notification)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentTitle(applicationContext.resources.getString(type.titleRes))
            .setContentText(type.formattedMessage(applicationContext, downloadedEpisodes))
            .setColor(ContextCompat.getColor(applicationContext, R.color.notification_color))
            .setAutoCancel(true)
            .setContentIntent(openPageIntent(type))
    }

    private fun openPageIntent(type: NotificationType): PendingIntent {
        return PendingIntent.getActivity(
            applicationContext,
            0,
            type.toIntent(applicationContext),
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
