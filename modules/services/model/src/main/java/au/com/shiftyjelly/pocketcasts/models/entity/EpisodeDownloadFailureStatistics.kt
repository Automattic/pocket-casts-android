package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import java.time.Instant

data class EpisodeDownloadFailureStatistics(
    @ColumnInfo(name = "count") val count: Long,
    @ColumnInfo(name = "newest_timestamp") val newestTimestamp: Instant?,
    @ColumnInfo(name = "oldest_timestamp") val oldestTimestamp: Instant?,
)
