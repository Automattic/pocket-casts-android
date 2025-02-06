package au.com.shiftyjelly.pocketcasts.models.to

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "episode_chapters",
    primaryKeys = ["episode_uuid", "chapter_index"],
    indices = [
        Index(name = "chapter_episode_uuid_index", value = ["episode_uuid"]),
    ],
)
data class DbChapter(
    @ColumnInfo(name = "chapter_index") val index: Int,
    @ColumnInfo(name = "episode_uuid") val episodeUuid: String,
    @ColumnInfo(name = "start_time") val startTimeMs: Long,
    @ColumnInfo(name = "end_time") val endTimeMs: Long? = null,
    @ColumnInfo(name = "title") val title: String? = null,
    @ColumnInfo(name = "image_url") val imageUrl: String? = null,
    @ColumnInfo(name = "url") val url: String? = null,
    @ColumnInfo(name = "is_embedded") val isEmbedded: Boolean = false,
)
