package au.com.shiftyjelly.pocketcasts.models.to

import androidx.room.ColumnInfo

data class PlaylistShortcut(
    @ColumnInfo(name = "uuid") val uuid: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "manual") val isManual: Boolean,
    @ColumnInfo(name = "iconId") val iconId: Int = 0,
) {
    val icon get() = PlaylistIcon(iconId)
}
