package au.com.shiftyjelly.pocketcasts.repositories.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.download.UpdateEpisodeDetailsTask
import au.com.shiftyjelly.pocketcasts.repositories.download.task.DownloadEpisodeTask
import au.com.shiftyjelly.pocketcasts.repositories.download.task.UploadEpisodeTask
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.opml.OpmlImportTask
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncHistoryTask
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.refresh.RefreshServerManager
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncSettingsTask
import javax.inject.Inject

class CastsWorkerFactory @Inject constructor(
    val podcastManager: PodcastManager,
    val episodeManager: EpisodeManager,
    val syncManager: SyncManager,
    val downloadManager: DownloadManager,
    val playbackManager: PlaybackManager,
    val refreshServerManager: RefreshServerManager,
    val notificationHelper: NotificationHelper,
    val settings: Settings,
    val userEpisodeManager: UserEpisodeManager
) : WorkerFactory() {
    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? {

        val workerKlass = Class.forName(workerClassName).asSubclass(ListenableWorker::class.java)
        val constructor = workerKlass.getDeclaredConstructor(Context::class.java, WorkerParameters::class.java)
        val instance = constructor.newInstance(appContext, workerParameters)

        when (instance) {
            is SyncHistoryTask -> {
                instance.episodeManager = episodeManager
                instance.syncManager = syncManager
                instance.podcastManager = podcastManager
                instance.settings = settings
            }
            is DownloadEpisodeTask -> {
                instance.downloadManager = downloadManager
                instance.episodeManager = episodeManager
                instance.userEpisodeManager = userEpisodeManager
            }
            is SyncSettingsTask -> {
                instance.namedSettingsCaller = syncManager
                instance.settings = settings
            }
            is UploadEpisodeTask -> {
                instance.userEpisodeManager = userEpisodeManager
                instance.playbackManager = playbackManager
            }
            is OpmlImportTask -> {
                instance.podcastManager = podcastManager
                instance.refreshServerManager = refreshServerManager
                instance.notificationHelper = notificationHelper
            }
            is UpdateEpisodeDetailsTask -> {
                instance.episodeManager = episodeManager
            }
        }

        return instance
    }
}
