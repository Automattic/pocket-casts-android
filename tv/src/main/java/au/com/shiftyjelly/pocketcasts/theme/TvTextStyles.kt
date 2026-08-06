package au.com.shiftyjelly.pocketcasts.theme

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The Figma type ramp for TV. Sizes are the Figma px values divided by 1.5 (the 1920x1080 → dp
 * convention), hence the fractional `sp`. Figma's Google Sans family is not ported (no such resource
 * in this module), so the tokens use the system default at [FontWeight] 500 (400 for [Subtitle1]).
 * The full ramp is defined even where a step has no call site yet, so the palette stays complete.
 */
object TvTextStyles {
    val Title1 = TextStyle(
        fontSize = 50.67.sp,
        lineHeight = 64.sp,
        fontWeight = FontWeight(500),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val Title2 = TextStyle(
        fontSize = 38.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight(500),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val Title3 = TextStyle(
        fontSize = 32.sp,
        lineHeight = 37.33.sp,
        fontWeight = FontWeight(500),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val Headline = TextStyle(
        fontSize = 25.33.sp,
        lineHeight = 30.67.sp,
        fontWeight = FontWeight(500),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val Subtitle1 = TextStyle(
        fontSize = 25.33.sp,
        lineHeight = 30.67.sp,
        fontWeight = FontWeight(400),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val Callout = TextStyle(
        fontSize = 19.33.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight(500),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val Body = TextStyle(
        fontSize = 16.67.sp,
        lineHeight = 21.33.sp,
        fontWeight = FontWeight(500),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val Caption1 = TextStyle(
        fontSize = 16.67.sp,
        lineHeight = 21.33.sp,
        fontWeight = FontWeight(500),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val Caption2 = TextStyle(
        fontSize = 15.33.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight(500),
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
}
