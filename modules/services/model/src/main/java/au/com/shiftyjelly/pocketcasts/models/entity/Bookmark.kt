package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable
import java.util.Date

@Entity(
    tableName = "bookmarks",
    indices = [
        Index(name = "podcast_uuid", value = arrayOf("podcast_uuid"))
    ]
)
data class Bookmark(
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "uuid") var uuid: String,
    @ColumnInfo(name = "podcast_uuid") var podcastUuid: String,
    @ColumnInfo(name = "episode_uuid") var episodeUuid: String,
    @ColumnInfo(name = "time") var timeSecs: Int,
    @ColumnInfo(name = "created_at") var createdAt: Date,
    @ColumnInfo(name = "deleted") var deleted: Boolean = false,
    @ColumnInfo(name = "sync_status") var syncStatus: Int
) : Serializable {

    companion object {
        const val SYNC_STATUS_NOT_SYNCED = 0
        const val SYNC_STATUS_SYNCED = 1
    }
}
