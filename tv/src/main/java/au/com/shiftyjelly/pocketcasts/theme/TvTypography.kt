package au.com.shiftyjelly.pocketcasts.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The Figma type ramp for TV. Sizes are the Figma px values divided by 2.0 (the 1920x1080 → 960x540
 * dp convention; Android TV renders at 320dpi = density 2.0), hence the fractional `sp`.
 */
@Immutable
data class TvTypography(
    val title1: TextStyle = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontSize = 38.sp,
        lineHeight = 48.sp,
        fontWeight = FontWeight(510),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val title2: TextStyle = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontSize = 28.5.sp,
        lineHeight = 33.sp,
        fontWeight = FontWeight(510),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val title3: TextStyle = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight(510),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val headline: TextStyle = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontSize = 19.sp,
        lineHeight = 23.sp,
        fontWeight = FontWeight(510),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val subtitle1: TextStyle = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontSize = 19.sp,
        lineHeight = 23.sp,
        fontWeight = FontWeight(400),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val callout: TextStyle = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontSize = 14.5.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight(510),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val body: TextStyle = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontSize = 14.5.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight(400),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val caption1: TextStyle = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontSize = 12.5.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight(510),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val caption2: TextStyle = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight(510),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
)
