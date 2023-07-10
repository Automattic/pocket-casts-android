package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import au.com.shiftyjelly.pocketcasts.models.type.SyncStatus
import java.io.Serializable
import java.util.Date

@Entity(
    tableName = "bookmarks",
    indices = [
        Index(name = "bookmarks_podcast_uuid", value = arrayOf("podcast_uuid"))
    ]
)
data class Bookmark(
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "uuid") var uuid: String,
    @ColumnInfo(name = "podcast_uuid") var podcastUuid: String,
    @ColumnInfo(name = "episode_uuid") var episodeUuid: String,
    @ColumnInfo(name = "time") var timeSecs: Int,
    @ColumnInfo(name = "created_at") var createdAt: Date,
    @ColumnInfo(name = "title") var title: String,
    @ColumnInfo(name = "title_modified") var titleModified: Long? = null,
    @ColumnInfo(name = "deleted") var deleted: Boolean = false,
    @ColumnInfo(name = "deleted_modified") var deletedModified: Long? = null,
    @ColumnInfo(name = "sync_status") var syncStatus: SyncStatus
) : Serializable
