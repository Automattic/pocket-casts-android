package au.com.shiftyjelly.pocketcasts.models.to

import androidx.room.ColumnInfo

data class DailyListenedTime(
    @ColumnInfo(name = "listen_date") val listenDate: String,
    @ColumnInfo(name = "total_played_seconds") val totalPlayedSeconds: Double,
)
