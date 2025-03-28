package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.io.Serializable
import java.util.Date

@Entity(
    tableName = "up_next_history",
    primaryKeys = ["episodeUuid", "addedDate"],
)
data class UpNextHistory(
    @ColumnInfo(name = "episodeUuid") var episodeUuid: String,
    @ColumnInfo(name = "position") var position: Int = 0,
    @ColumnInfo(name = "playlistId") var playlistId: Long? = null,
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "publishedDate") var publishedDate: Date? = null,
    @ColumnInfo(name = "downloadUrl") var downloadUrl: String? = null,
    @ColumnInfo(name = "podcastUuid") var podcastUuid: String? = null,
    @ColumnInfo(name = "addedDate") var addedDate: Date = Date(),
) : Serializable
