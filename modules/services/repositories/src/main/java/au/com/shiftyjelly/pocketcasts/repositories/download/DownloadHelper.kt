package au.com.shiftyjelly.pocketcasts.repositories.download

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.file.StorageException
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import kotlinx.coroutines.runBlocking

object DownloadHelper {

    fun manuallyDownloadEpisodeNow(
        episode: BaseEpisode,
        from: String,
        downloadManager: DownloadManager,
        episodeManager: EpisodeManager,
        fireToast: Boolean = false,
        source: SourceView,
    ) {
        if (episode.isDownloaded) {
            return
        }

        runBlocking {
            episodeManager.updateAutoDownloadStatus(episode, PodcastEpisode.AUTO_DOWNLOAD_STATUS_MANUALLY_DOWNLOADED)
        }
        downloadManager.addEpisodeToQueue(episode, from, fireEvent = true, fireToast = fireToast, source = source)
    }

    fun addAutoDownloadedEpisodeToQueue(episode: BaseEpisode, from: String, downloadManager: DownloadManager, episodeManager: EpisodeManager, source: SourceView) {
        if (episode.isDownloaded || episode.episodeStatus == EpisodeStatusEnum.DOWNLOAD_FAILED) {
            if (episode.episodeStatus == EpisodeStatusEnum.DOWNLOAD_FAILED) {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Not autodownloading ${episode.title} from $from because it has already failed.")
            }
            return
        }
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Adding ${episode.title} to auto download from $from")
        runBlocking {
            episodeManager.updateAutoDownloadStatus(episode, PodcastEpisode.AUTO_DOWNLOAD_STATUS_AUTO_DOWNLOADED)
        }
        downloadManager.addEpisodeToQueue(episode, from, fireEvent = true, fireToast = false, source = source)
    }

    fun removeEpisodeFromQueue(episode: BaseEpisode, from: String, downloadManager: DownloadManager) {
        downloadManager.removeEpisodeFromQueue(episode, from)
    }

    @Throws(StorageException::class)
    fun pathForEpisode(episode: BaseEpisode, fileStorage: FileStorage): String? {
        val file = fileStorage.getOrCreatePodcastEpisodeFile(episode)
        return file?.absolutePath
    }

    @Throws(StorageException::class)
    fun tempPathForEpisode(episode: BaseEpisode, fileStorage: FileStorage): String {
        val file = fileStorage.getOrCreatePodcastEpisodeTempFile(episode)
        return file.absolutePath
    }
}
