package au.com.shiftyjelly.pocketcasts.models.type

import java.io.File
import java.util.UUID

sealed interface DownloadStatusUpdate {
    val taskId: UUID
    val episodeStatus: EpisodeDownloadStatus
    val outputFile: File?
    val errorMessage: String?

    data class Cancelled(
        override val taskId: UUID,
    ) : DownloadStatusUpdate {
        override val episodeStatus get() = EpisodeDownloadStatus.DownloadNotRequested
        override val outputFile get() = null
        override val errorMessage get() = null
    }

    data class WaitingForWifi(
        override val taskId: UUID,
    ) : DownloadStatusUpdate {
        override val episodeStatus get() = EpisodeDownloadStatus.WaitingForWifi
        override val outputFile get() = null
        override val errorMessage get() = null
    }

    data class WaitingForPower(
        override val taskId: UUID,
    ) : DownloadStatusUpdate {
        override val episodeStatus get() = EpisodeDownloadStatus.WaitingForPower
        override val outputFile get() = null
        override val errorMessage get() = null
    }

    data class WaitingForStorage(
        override val taskId: UUID,
    ) : DownloadStatusUpdate {
        override val episodeStatus get() = EpisodeDownloadStatus.WaitingForStorage
        override val outputFile get() = null
        override val errorMessage get() = null
    }

    data class Enqueued(
        override val taskId: UUID,
    ) : DownloadStatusUpdate {
        override val episodeStatus get() = EpisodeDownloadStatus.Queued
        override val outputFile get() = null
        override val errorMessage get() = null
    }

    data class InProgress(
        override val taskId: UUID,
    ) : DownloadStatusUpdate {
        override val episodeStatus get() = EpisodeDownloadStatus.Downloading
        override val outputFile get() = null
        override val errorMessage get() = null
    }

    data class Success(
        override val taskId: UUID,
        override val outputFile: File,
    ) : DownloadStatusUpdate {
        override val episodeStatus get() = EpisodeDownloadStatus.Downloaded
        override val errorMessage get() = null
    }

    data class Failure(
        override val taskId: UUID,
        override val errorMessage: String,
    ) : DownloadStatusUpdate {
        override val episodeStatus get() = EpisodeDownloadStatus.DownloadFailed
        override val outputFile get() = null
    }
}
