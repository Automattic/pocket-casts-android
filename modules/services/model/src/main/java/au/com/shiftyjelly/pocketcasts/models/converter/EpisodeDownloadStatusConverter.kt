package au.com.shiftyjelly.pocketcasts.models.converter

import androidx.room.TypeConverter
import au.com.shiftyjelly.pocketcasts.models.type.DownloadStatusUpdate
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus.DownloadFailed
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus.DownloadNotRequested
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus.Downloaded
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus.Downloading
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus.Queued
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus.QueuedRetry
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus.WaitingForPower
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus.WaitingForStorage
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus.WaitingForWifi

class EpisodeDownloadStatusConverter {
    @TypeConverter
    fun fromInt(value: Int?): EpisodeDownloadStatus {
        return if (value != null) {
            DB_VALUE_MAP[value] ?: DownloadNotRequested
        } else {
            DownloadNotRequested
        }
    }

    @TypeConverter
    fun toInt(value: EpisodeDownloadStatus?): Int {
        return value?.dbValue ?: DownloadNotRequested.dbValue
    }
}

// Legacy values resulting from ordinals
private val EpisodeDownloadStatus.dbValue get() = when (this) {
    DownloadNotRequested -> 0
    Queued -> 1
    Downloading -> 2
    DownloadFailed -> 3
    Downloaded -> 4
    WaitingForWifi -> 5
    WaitingForPower -> 6
    WaitingForStorage -> 7
    QueuedRetry -> 8
}

private val DB_VALUE_MAP = EpisodeDownloadStatus.entries.associateBy { it.dbValue }
