package au.com.shiftyjelly.pocketcasts.theme

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object TvTextStyles {
    val Title1 = TextStyle(
        fontSize = 50.67.sp,
        lineHeight = 64.sp,
        fontWeight = FontWeight(510),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val Title2 = TextStyle(
        fontSize = 38.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight(510),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val Title3 = TextStyle(
        fontSize = 32.sp,
        lineHeight = 37.33.sp,
        fontWeight = FontWeight(510),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val Headline = TextStyle(
        fontSize = 25.33.sp,
        lineHeight = 30.67.sp,
        fontWeight = FontWeight(510),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val Subtitle1 = TextStyle(
        fontSize = 25.33.sp,
        lineHeight = 30.67.sp,
        fontWeight = FontWeight(400),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val Callout = TextStyle(
        fontSize = 19.33.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight(510),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val Body = TextStyle(
        fontSize = 19.33.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight(400),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val Caption1 = TextStyle(
        fontSize = 16.67.sp,
        lineHeight = 21.33.sp,
        fontWeight = FontWeight(510),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val Caption2 = TextStyle(
        fontSize = 15.33.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight(510),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
}
