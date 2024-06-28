package au.com.shiftyjelly.pocketcasts.repositories.download

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.content.getSystemService
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.onEach
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltWorker
class FixDownloadsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val episodeManager: EpisodeManager,
    private val fileStorage: FileStorage,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        analyticsTracker.track(AnalyticsEvent.SETTINGS_STORAGE_FIX_DOWNLOADED_FILES_START)

        val episodeCount = episodeManager.countEpisodes()
        initializeForegroundInfo(episodeCount)
        val fixedCount = fixDownloads(episodeCount)
        showCompleteNotification(fixedCount)

        analyticsTracker.track(AnalyticsEvent.SETTINGS_STORAGE_FIX_DOWNLOADED_FILES_END, mapOf("fixed_count" to fixedCount))
        return Result.success()
    }

    private suspend fun fixDownloads(episodeCount: Int) = episodeManager
        .getAllPodcastEpisodes(pageLimit = 10_000)
        .onEach { (_, index) -> updateProgressNotification(progress = index + 1, total = episodeCount) }
        .fold(0) { fixedCount, (episode, _) ->
            val path = findExpectedDownloadPath(episode)
            if (path != null && path != episode.downloadedFilePath) {
                episodeManager.updateDownloadFilePath(episode, path, markAsDownloaded = true)
                fixedCount + 1
            } else {
                fixedCount
            }
        }

    private fun findExpectedDownloadPath(episode: PodcastEpisode) = DownloadHelper.pathForEpisode(episode, fileStorage)?.takeIf {
        val file = File(it)
        file.exists() && file.isFile
    }

    private suspend fun initializeForegroundInfo(totalEpisodeCount: Int) {
        val notificationId = Settings.NotificationId.FIX_DOWNLOADS.value
        val notification = buildProgressNotification(progress = 0, total = totalEpisodeCount)
        val foregroundInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
        setForeground(foregroundInfo)
    }

    private fun updateProgressNotification(progress: Int, total: Int) {
        // Updating for every episode makes the whole process extremely slow.
        // It also makes "Stop" button unresponsive because notification is updated so often.
        // Sampling it makes things easier.
        if (isStopped || progress % 1_000 != 0) {
            return
        }
        val manager = requireNotNull(applicationContext.getSystemService<NotificationManager>()) {
            "Notification manager not found"
        }
        manager.notify(Settings.NotificationId.FIX_DOWNLOADS.value, buildProgressNotification(progress, total))
    }

    private fun buildProgressNotification(progress: Int, total: Int): Notification {
        val cancelIntent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
        return notificationHelper.downloadsFixChannelBuilder()
            .setSmallIcon(IR.drawable.notification)
            .setContentTitle(applicationContext.getString(LR.string.settings_fix_downloads_title))
            .setContentText(applicationContext.getString(LR.string.settings_fix_downloads_progress, progress, total))
            .setProgress(total, progress, false)
            .setOngoing(true)
            .addAction(IR.drawable.ic_cancel, applicationContext.getString(LR.string.stop), cancelIntent)
            .build()
    }

    private fun showCompleteNotification(fixed: Int) {
        val manager = requireNotNull(applicationContext.getSystemService<NotificationManager>()) {
            "Notification manager not found"
        }
        val notification = notificationHelper.downloadsFixCompleteChannelBuilder()
            .setSmallIcon(IR.drawable.notification)
            .setContentTitle(applicationContext.getString(LR.string.settings_fix_downloads_complete_title))
            .setContentText(applicationContext.getString(LR.string.settings_fix_downloads_complete_message, fixed))
            .setOngoing(false)
            .build()
        manager.notify(Settings.NotificationId.FIX_DOWNLOADS_COMPLETE.value, notification)
    }

    companion object {
        private const val TASK_NAME = "fix_downloads_task"

        fun run(context: Context) {
            val request = OneTimeWorkRequestBuilder<FixDownloadsWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(TASK_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
