package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "user_podcast_ratings")
data class UserPodcastRating(
    @PrimaryKey @ColumnInfo(name = "podcast_uuid") val podcastUuid: String,
    @ColumnInfo(name = "rating") val rating: Int,
    @ColumnInfo(name = "modified_at") val modifiedAt: Date,
)
