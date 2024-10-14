package au.com.shiftyjelly.pocketcasts.models.to

import androidx.room.ColumnInfo

data class PodcastRatingGrouping(
    @ColumnInfo(name = "rating") val rating: Int,
    @ColumnInfo(name = "count") val count: Int,
)
