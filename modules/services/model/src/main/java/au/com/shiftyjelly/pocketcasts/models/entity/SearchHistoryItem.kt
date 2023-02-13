package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "search_history",
    indices = [
        Index(value = ["term"], unique = true),
        Index(value = ["podcast_uuid"], unique = true),
        Index(value = ["folder_uuid"], unique = true),
        Index(value = ["episode_uuid"], unique = true),
    ]
)
data class SearchHistoryItem(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") var id: Long? = null,
    @ColumnInfo(name = "modified") var modified: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "subscribed") var isSubscribed: Boolean = false,
    @ColumnInfo(name = "term") var term: String? = null,
    @Embedded(prefix = "podcast_") var podcast: Podcast? = null,
    @Embedded(prefix = "folder_") var folder: Folder? = null,
    @Embedded(prefix = "episode_") var episode: Episode? = null,
) {
    data class Podcast(
        val uuid: String,
        val title: String,
        val author: String,
    )
    data class Folder(
        val uuid: String,
        val title: String,
        val color: Int,
        val podcastIds: String,
    )
    data class Episode(
        val uuid: String,
        val title: String,
        val publishedDate: Date,
        val duration: Double
    )
}
