package au.com.shiftyjelly.pocketcasts.models.to

import androidx.room.ColumnInfo

data class EpisodeWithTitle(
    @ColumnInfo(name = "uuid") val uuid: String,
    @ColumnInfo(name = "title") val title: String,
)
