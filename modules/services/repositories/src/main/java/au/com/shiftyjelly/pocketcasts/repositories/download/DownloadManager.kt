package au.com.shiftyjelly.pocketcasts.repositories.download

import android.content.Context
import androidx.core.app.NotificationCompat
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import kotlinx.coroutines.flow.Flow

interface DownloadManager {
    companion object {
        const val WORK_MANAGER_DOWNLOAD_TAG = "downloadTask"
    }

    fun updateEpisodeDownloadProgress(episodeUuid: String, progress: DownloadProgressUpdate)

    fun episodeDownloadProgressFlow(episodeUuid: String): Flow<DownloadProgressUpdate>

    fun setup(episodeManager: EpisodeManager, podcastManager: PodcastManager, playbackManager: PlaybackManager)
    fun beginMonitoringWorkManager(context: Context)
    fun hasPendingOrRunningDownloads(): Boolean
    fun addEpisodeToQueue(episode: BaseEpisode, from: String, fireEvent: Boolean, source: SourceView)
    fun removeEpisodeFromQueue(episode: BaseEpisode, from: String)
    fun stopAllDownloads()
    suspend fun getRequirementsAndSetStatusAsync(episode: BaseEpisode): NetworkRequirements
    fun getNotificationBuilder(): NotificationCompat.Builder
}
