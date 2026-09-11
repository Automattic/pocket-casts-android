package au.com.shiftyjelly.pocketcasts.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class TvColorScheme(
    val textPrimary: Color = Color(0xFFFBFBFC),
    val textSecondary: Color = Color(0xFFB0B3B8),
    val textTertiary: Color = Color(0xFF7A7D82),
    val textDisabled: Color = Color(0xFF4A4D51),
    val textPrimaryActive: Color = Color(0xFF161718),
    val textSecondaryActive: Color = Color(0xFF3D4044),
    val textTertiaryActive: Color = Color(0xFF7A7D82),
    val textDisabledActive: Color = Color(0xFFB0B3B8),
    val textPrimary70: Color = Color(0xB3FBFBFC),
    val backgroundSunken: Color = Color(0xFF161718),
    val backgroundSurface: Color = Color(0xFF1F2123),
    val backgroundBase: Color = Color(0xFF292B2E),
    val backgroundOverlay: Color = Color(0xFF323538),
    val backgroundActive: Color = Color(0xFFD4D6DB),
    val backgroundActive50: Color = backgroundActive.copy(alpha = 0.5f),
    val backgroundActive20: Color = backgroundActive.copy(alpha = 0.2f),
    val overlayContainer: Color = backgroundSunken.copy(alpha = 0.94f),
    val translucentOverlayContainer: Color = backgroundSunken.copy(alpha = 0.6f),
    val overlayBorder: Color = backgroundActive.copy(alpha = 0.12f),
)
