package au.com.shiftyjelly.pocketcasts.models.to

import androidx.room.ColumnInfo

data class SmartPlaylistMetadata(
    @ColumnInfo("episode_count") val episodeCount: Int,
    @ColumnInfo("time_left") val timeLeftSeconds: Double,
)
