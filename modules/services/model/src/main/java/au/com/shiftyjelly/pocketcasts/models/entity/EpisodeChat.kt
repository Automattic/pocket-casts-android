package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "episode_chats",
    foreignKeys = [
        ForeignKey(
            entity = PodcastEpisode::class,
            parentColumns = ["uuid"],
            childColumns = ["episode_uuid"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class EpisodeChat(
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "episode_uuid") val episodeUuid: String,
    @ColumnInfo(name = "podcast_uuid") val podcastUuid: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Date = Date(),
)
