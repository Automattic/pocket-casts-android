package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo

data class NovaLauncherTrendingPodcast(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "title") val title: String,
)
