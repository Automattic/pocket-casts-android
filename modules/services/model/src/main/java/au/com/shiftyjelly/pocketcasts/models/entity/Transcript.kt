package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "episode_transcript",
    primaryKeys = ["episode_uuid", "url"],
)
data class Transcript(
    @ColumnInfo(name = "episode_uuid") val episodeUuid: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "is_generated") val isGenerated: Boolean,
    @ColumnInfo(name = "language") val language: String? = null,
)
