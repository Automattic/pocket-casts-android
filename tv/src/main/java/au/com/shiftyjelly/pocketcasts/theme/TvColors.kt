package au.com.shiftyjelly.pocketcasts.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object TvColors {
    val TextPrimary = Color(0xFFFBFBFC)
    val TextSecondary = Color(0xFFB0B3B8)
    val TextTertiary = Color(0xFF7A7D82)
    val TextDisabled = Color(0xFF4A4D51)
    val TextPrimaryActive = Color(0xFF161718)
    val TextSecondaryActive = Color(0xFF3D4044)
    val TextTertiaryActive = Color(0xFF7A7D82)
    val TextDisabledActive = Color(0xFFB0B3B8)
    val TextPrimary70 = Color(0xB3FBFBFC)

    val BackgroundSunken = Color(0xFF161718)
    val BackgroundSurface = Color(0xFF1F2123)
    val BackgroundBase = Color(0xFF292B2E)
    val BackgroundOverlay = Color(0xFF323538)
    val BackgroundActive = Color(0xFFFBFBFC)
    val BackgroundActive50 = Color(0x80FBFBFC)

    val Divider = Color(0xFF4A4D51)

    val Dark = Color(0xFF161718)
    val DarkGray = Color(0xFF292B2E)
    val Gray = Color(0xFF3C3E42)
    val LightGray = Color(0xFFD4D6DB)
    val TextPrimary20 = Color(0x33FBFBFC)
    val BgActive = Color(0xFFD4D6DB)
    val BgActive20 = Color(0x33FBFBFC)
}

// Shared screen background so detail overlays fully occlude the content fading underneath them.
val TvScreenBackgroundBrush: Brush = Brush.horizontalGradient(
    colors = listOf(TvColors.BackgroundBase, TvColors.BackgroundSunken),
)
