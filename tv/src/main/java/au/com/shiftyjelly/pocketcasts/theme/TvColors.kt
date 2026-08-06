package au.com.shiftyjelly.pocketcasts.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object TvColors {
    val Dark = Color(0xFF161718)
    val DarkGray = Color(0xFF292B2E)
    val Gray = Color(0xFF3C3E42)
    val LightGray = Color(0xFFD4D6DB)
    val TextPrimary = Color(0xFFFBFBFC)
    val TextPrimary20 = Color(0x33FBFBFC)
    val TextSecondary = Color(0xFFB0B3B8)
    val TextSecondaryActive = Color(0xFF3D4044)
    val BgActive = Color(0xFFD4D6DB)
    val BgActive20 = Color(0x33FBFBFC)
    val Divider = Color(0xFF4A4D51)
}

// Shared screen background so detail overlays fully occlude the content fading underneath them.
val TvScreenBackgroundBrush: Brush = Brush.horizontalGradient(
    colors = listOf(TvColors.DarkGray, TvColors.Dark),
)
