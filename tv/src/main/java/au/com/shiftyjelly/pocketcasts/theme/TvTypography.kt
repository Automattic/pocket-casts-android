package au.com.shiftyjelly.pocketcasts.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The Figma type ramp for TV. Sizes are the Figma px values divided by 1.5 (the 1920x1080 → dp
 * convention), hence the fractional `sp`.
 */
@Immutable
data class TvTypography(
    val title1: TextStyle = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontSize = 50.67.sp,
        lineHeight = 64.sp,
        fontWeight = FontWeight(510),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val title2: TextStyle = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontSize = 38.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight(510),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val title3: TextStyle = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontSize = 32.sp,
        lineHeight = 37.33.sp,
        fontWeight = FontWeight(510),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val headline: TextStyle = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontSize = 25.33.sp,
        lineHeight = 30.67.sp,
        fontWeight = FontWeight(510),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val subtitle1: TextStyle = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontSize = 25.33.sp,
        lineHeight = 30.67.sp,
        fontWeight = FontWeight(400),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val callout: TextStyle = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontSize = 19.33.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight(510),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val body: TextStyle = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontSize = 19.33.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight(400),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val caption1: TextStyle = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontSize = 16.67.sp,
        lineHeight = 21.33.sp,
        fontWeight = FontWeight(510),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val caption2: TextStyle = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontSize = 15.33.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight(510),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
)
