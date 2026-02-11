package au.com.shiftyjelly.pocketcasts.models.type

import java.io.File

sealed interface DownloadStatusUpdate {
    val episodeStatus: EpisodeDownloadStatus
    val outputFile: File?
    val errorMessage: String?

    data object Idle : DownloadStatusUpdate {
        override val episodeStatus get() = EpisodeDownloadStatus.DownloadNotRequested
        override val outputFile get() = null
        override val errorMessage get() = null
    }

    data object WaitingForWifi : DownloadStatusUpdate {
        override val episodeStatus get() = EpisodeDownloadStatus.WaitingForWifi
        override val outputFile get() = null
        override val errorMessage get() = null
    }

    data object WaitingForPower : DownloadStatusUpdate {
        override val episodeStatus get() = EpisodeDownloadStatus.WaitingForPower
        override val outputFile get() = null
        override val errorMessage get() = null
    }

    data object Enqueued : DownloadStatusUpdate {
        override val episodeStatus get() = EpisodeDownloadStatus.Queued
        override val outputFile get() = null
        override val errorMessage get() = null
    }

    data object InProgress : DownloadStatusUpdate {
        override val episodeStatus get() = EpisodeDownloadStatus.Downloading
        override val outputFile get() = null
        override val errorMessage get() = null
    }

    data class Success(
        override val outputFile: File,
    ) : DownloadStatusUpdate {
        override val episodeStatus get() = EpisodeDownloadStatus.Downloaded
        override val errorMessage get() = null
    }

    data class Failure(
        override val errorMessage: String,
    ) : DownloadStatusUpdate {
        override val episodeStatus get() = EpisodeDownloadStatus.DownloadFailed
        override val outputFile get() = null
    }
}
