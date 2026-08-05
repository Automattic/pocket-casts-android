package au.com.shiftyjelly.pocketcasts.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

@Immutable
data class TvTypography(
    val title1: TextStyle = TextStyle(
        fontSize = 50.67.sp,
        lineHeight = 64.sp,
        fontWeight = FontWeight(510),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val title2: TextStyle = TextStyle(
        fontSize = 38.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight(510),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val title3: TextStyle = TextStyle(
        fontSize = 32.sp,
        lineHeight = 37.33.sp,
        fontWeight = FontWeight(510),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val headline: TextStyle = TextStyle(
        fontSize = 25.33.sp,
        lineHeight = 30.67.sp,
        fontWeight = FontWeight(510),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val subtitle1: TextStyle = TextStyle(
        fontSize = 25.33.sp,
        lineHeight = 30.67.sp,
        fontWeight = FontWeight(400),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val callout: TextStyle = TextStyle(
        fontSize = 19.33.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight(510),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val caption1: TextStyle = TextStyle(
        fontSize = 16.67.sp,
        lineHeight = 21.33.sp,
        fontWeight = FontWeight(510),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val caption2: TextStyle = TextStyle(
        fontSize = 15.33.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight(510),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val videoTilePodcastTitle: TextStyle = TextStyle(
        fontSize = 12.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val videoTileEpisodeTitle: TextStyle = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val featuredTileSponsoredLabel: TextStyle = TextStyle(
        fontSize = 14.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val featuredTileTitle: TextStyle = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val featuredTileDescription: TextStyle = TextStyle(
        fontSize = 16.sp,
        lineHeight = 20.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val welcomeTitle: TextStyle = TextStyle(
        fontSize = 27.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 27.sp,
        letterSpacing = (-0.25).sp,
        textAlign = TextAlign.Center,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val welcomeSubtitle: TextStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
        letterSpacing = (-0.04).sp,
        textAlign = TextAlign.Center,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val signInSubtitle: TextStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 18.sp,
        textAlign = TextAlign.Center,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
    val modalBody: TextStyle = TextStyle(
        fontSize = 16.sp,
        lineHeight = 20.sp,
        textAlign = TextAlign.Center,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ),
)
