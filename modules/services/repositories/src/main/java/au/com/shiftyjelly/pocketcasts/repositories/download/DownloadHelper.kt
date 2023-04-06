package au.com.shiftyjelly.pocketcasts.repositories.download

import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.file.StorageException
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer

object DownloadHelper {

    fun manuallyDownloadEpisodeNow(episode: Playable, from: String, downloadManager: DownloadManager, episodeManager: EpisodeManager) {
        if (episode.isDownloaded) {
            return
        }

        episodeManager.updateAutoDownloadStatus(episode, Episode.AUTO_DOWNLOAD_STATUS_MANUALLY_DOWNLOADED)
        downloadManager.addEpisodeToQueue(episode, from, true)
    }

    fun addAutoDownloadedEpisodeToQueue(episode: Playable, from: String, downloadManager: DownloadManager, episodeManager: EpisodeManager) {
        if (episode.isDownloaded || episode.episodeStatus == EpisodeStatusEnum.DOWNLOAD_FAILED) {
            if (episode.episodeStatus == EpisodeStatusEnum.DOWNLOAD_FAILED) {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Not autodownloading ${episode.title} from $from because it has already failed.")
            }
            return
        }
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Adding ${episode.title} to auto download from $from")
        episodeManager.updateAutoDownloadStatus(episode, Episode.AUTO_DOWNLOAD_STATUS_AUTO_DOWNLOADED)
        downloadManager.addEpisodeToQueue(episode, from, true)
    }

    fun addEpisodeToQueueOverridingWifiWarning(episode: Episode, from: String, downloadManager: DownloadManager, episodeManager: EpisodeManager) {
        if (episode.isDownloaded) {
            return
        }
        episodeManager.updateAutoDownloadStatus(episode, Episode.AUTO_DOWNLOAD_STATUS_MANUAL_OVERRIDE_WIFI)
        downloadManager.addEpisodeToQueue(episode, from, true)
    }

    fun removeEpisodeFromQueue(episode: Playable, from: String, downloadManager: DownloadManager) {
        downloadManager.removeEpisodeFromQueue(episode, from)
    }

    @Throws(StorageException::class)
    fun pathForEpisode(episode: Playable, fileStorage: FileStorage): String? {
        val file = fileStorage.getPodcastEpisodeFile(episode)
        return file.absolutePath
    }

    @Throws(StorageException::class)
    fun tempPathForEpisode(episode: Playable, fileStorage: FileStorage): String {
        val file = fileStorage.getTempPodcastEpsisodeFile(episode)
        return file.absolutePath
    }
}
