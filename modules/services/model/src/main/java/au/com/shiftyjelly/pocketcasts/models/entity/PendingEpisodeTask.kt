package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import java.time.Instant

/** A step that still has to run for a newly refreshed episode, so it survives the process being killed mid-refresh. */
@Entity(
    tableName = "pending_episode_tasks",
    primaryKeys = ["episode_uuid", "task"],
    foreignKeys = [
        ForeignKey(
            entity = PodcastEpisode::class,
            parentColumns = ["uuid"],
            childColumns = ["episode_uuid"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PendingEpisodeTask(
    @ColumnInfo(name = "episode_uuid") val episodeUuid: String,
    @ColumnInfo(name = "task") val task: Type,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
) {
    enum class Type {
        AUTO_DOWNLOAD,
        UP_NEXT,
    }
}
