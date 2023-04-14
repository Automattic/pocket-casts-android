package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "podcast_ratings",
)
data class PodcastRatings(
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "podcast_uuid") var podcastUuid: String,
    @ColumnInfo(name = "average") var average: Double,
    @ColumnInfo(name = "total") var total: Int? = null,
)
