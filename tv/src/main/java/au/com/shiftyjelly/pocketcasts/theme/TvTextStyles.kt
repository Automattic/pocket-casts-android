package au.com.shiftyjelly.pocketcasts.theme

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    val ScreenTitle = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val PlaylistCardTitle = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 28.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val PlaylistCardCaption = TextStyle(
        fontSize = 15.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val Caption = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight(510),
        lineHeight = 21.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val FolderCardTitle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val EpisodeRowTitle = TextStyle(
        fontSize = 19.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 24.sp,
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

    val ModalTitle = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 28.sp,
        textAlign = TextAlign.Center,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val ModalBody = TextStyle(
        fontSize = 16.sp,
        lineHeight = 20.sp,
        textAlign = TextAlign.Center,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val ModalEmail = TextStyle(
        fontSize = 25.sp,
        fontWeight = FontWeight(510),
        lineHeight = 31.sp,
        textAlign = TextAlign.Center,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val ModalButtonLabel = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight(510),
        lineHeight = 21.sp,
        textAlign = TextAlign.Center,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    val ModalFootnote = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight(510),
        lineHeight = 21.sp,
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
