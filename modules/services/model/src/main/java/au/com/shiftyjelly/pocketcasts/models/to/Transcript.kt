package au.com.shiftyjelly.pocketcasts.models.to

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "episode_transcript",
    primaryKeys = ["episode_uuid", "url"],
    indices = [
        Index(name = "transcript_episode_uuid_index", value = ["episode_uuid"], unique = true),
    ],
)
data class Transcript(
    @ColumnInfo(name = "episode_uuid") val episodeUuid: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "language") val language: String? = null,
)
