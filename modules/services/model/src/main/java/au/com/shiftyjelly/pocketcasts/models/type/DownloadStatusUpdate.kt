package au.com.shiftyjelly.pocketcasts.models.type

import java.io.File

sealed interface DownloadStatusUpdate {
    val episodeStatus: EpisodeStatusEnum
    val outputFile: File?
    val errorMessage: String?

    data object Idle : DownloadStatusUpdate {
        override val episodeStatus get() = EpisodeStatusEnum.NOT_DOWNLOADED
        override val outputFile get() = null
        override val errorMessage get() = null
    }

    data object WaitingForWifi : DownloadStatusUpdate {
        override val episodeStatus get() = EpisodeStatusEnum.WAITING_FOR_WIFI
        override val outputFile get() = null
        override val errorMessage get() = null
    }

    data object WaitingForPower : DownloadStatusUpdate {
        override val episodeStatus get() = EpisodeStatusEnum.WAITING_FOR_POWER
        override val outputFile get() = null
        override val errorMessage get() = null
    }

    data object Enqueued : DownloadStatusUpdate {
        override val episodeStatus get() = EpisodeStatusEnum.QUEUED
        override val outputFile get() = null
        override val errorMessage get() = null
    }

    data object InProgress : DownloadStatusUpdate {
        override val episodeStatus get() = EpisodeStatusEnum.DOWNLOADING
        override val outputFile get() = null
        override val errorMessage get() = null
    }

    data class Success(
        override val outputFile: File,
    ) : DownloadStatusUpdate {
        override val episodeStatus get() = EpisodeStatusEnum.DOWNLOADED
        override val errorMessage get() = null
    }

    data class Failure(
        override val errorMessage: String,
    ) : DownloadStatusUpdate {
        override val episodeStatus get() = EpisodeStatusEnum.DOWNLOAD_FAILED
        override val outputFile get() = null
    }
}
