package au.com.shiftyjelly.pocketcasts.theme

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

object TvTextStyles {
    val TabLabel = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight(510),
        lineHeight = 21.sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Center,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val VideoTilePodcastTitle = TextStyle(
        fontSize = 12.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val VideoTileEpisodeTitle = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val FeaturedTileSponsoredLabel = TextStyle(
        fontSize = 14.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val FeaturedTileTitle = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val FeaturedTileDescription = TextStyle(
        fontSize = 16.sp,
        lineHeight = 20.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val WelcomeTitle = TextStyle(
        fontSize = 27.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 27.sp,
        letterSpacing = (-0.25).sp,
        textAlign = TextAlign.Center,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val SignInSubtitle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 18.sp,
        textAlign = TextAlign.Center,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val WelcomeSubtitle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
        letterSpacing = (-0.04).sp,
        textAlign = TextAlign.Center,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
}
