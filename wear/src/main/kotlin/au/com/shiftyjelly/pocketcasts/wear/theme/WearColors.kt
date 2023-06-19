package au.com.shiftyjelly.pocketcasts.wear.theme

import androidx.compose.ui.graphics.Color
import au.com.shiftyjelly.pocketcasts.compose.ThemeExtraDarkColors
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.extensions.colorIndex

object WearColors {
    val highlight = Color(0xFFF43E37)
    val success = Color(0xFFA1E7B0)
    val surface = Color(0xFF202124)
    val primaryText = Color.White
    val secondaryText = Color(0xFFBDC1C6)
    val upNextIcon = Color(0xFF4BA1D6)
    val downloadedIcon = Color(0xFF54C483)

    fun getFolderColor(id: Int): Color = ThemeExtraDarkColors.getFolderColor(id)
    fun getFilterColor(playlist: Playlist): Color = ThemeExtraDarkColors.getFilterColor(playlist.colorIndex)
}
