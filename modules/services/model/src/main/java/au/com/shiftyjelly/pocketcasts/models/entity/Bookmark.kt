package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import au.com.shiftyjelly.pocketcasts.models.type.SyncStatus
import java.io.Serializable
import java.util.Calendar
import java.util.Date
import java.util.UUID

@Entity(
    tableName = "bookmarks",
    indices = [
        Index(name = "bookmarks_podcast_uuid", value = arrayOf("podcast_uuid"))
    ]
)
data class Bookmark(
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "uuid") var uuid: String,
    @ColumnInfo(name = "podcast_uuid") var podcastUuid: String = "",
    @ColumnInfo(name = "episode_uuid") var episodeUuid: String = "",
    @ColumnInfo(name = "time") var timeSecs: Int = 0,
    @ColumnInfo(name = "created_at") var createdAt: Date = Date(),
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "title_modified") var titleModified: Long? = null,
    @ColumnInfo(name = "deleted") var deleted: Boolean = false,
    @ColumnInfo(name = "deleted_modified") var deletedModified: Long? = null,
    @ColumnInfo(name = "sync_status") var syncStatus: SyncStatus = SyncStatus.NOT_SYNCED,
    @Ignore val episodeTitle: String = "",
) : Serializable {
    constructor() : this(uuid = "")

    val adapterId: Long
        get() = UUID.nameUUIDFromBytes(uuid.toByteArray()).mostSignificantBits

    fun createdAtDatePattern(): String {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        calendar.time = createdAt
        val createdAtYear = calendar.get(Calendar.YEAR)

        return if (createdAtYear == currentYear) {
            "MMM d 'at' h:mm a"
        } else {
            "MMM d, YYYY 'at' h:mm a"
        }
    }
}
