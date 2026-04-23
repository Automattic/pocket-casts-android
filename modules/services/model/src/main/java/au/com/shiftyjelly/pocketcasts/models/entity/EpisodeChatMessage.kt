package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "episode_chat_messages",
    indices = [
        Index(name = "episode_chat_messages_episode_uuid", value = ["episode_uuid"]),
    ],
)
data class EpisodeChatMessage(
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "uuid") val uuid: String,
    @ColumnInfo(name = "episode_uuid") val episodeUuid: String,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "role") val role: String,
    @ColumnInfo(name = "created_at") val createdAt: Date = Date(),
)
