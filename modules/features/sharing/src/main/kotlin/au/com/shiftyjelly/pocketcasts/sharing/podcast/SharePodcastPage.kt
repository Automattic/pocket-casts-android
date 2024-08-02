package au.com.shiftyjelly.pocketcasts.sharing.podcast

import android.content.res.Configuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.sharing.SharingResponse
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform
import au.com.shiftyjelly.pocketcasts.sharing.ui.CardType
import au.com.shiftyjelly.pocketcasts.sharing.ui.Devices
import au.com.shiftyjelly.pocketcasts.sharing.ui.HorizontalPodcastCast
import au.com.shiftyjelly.pocketcasts.sharing.ui.HorizontalSharePage
import au.com.shiftyjelly.pocketcasts.sharing.ui.ShareColors
import au.com.shiftyjelly.pocketcasts.sharing.ui.SquarePodcastCast
import au.com.shiftyjelly.pocketcasts.sharing.ui.VerticalPodcastCast
import au.com.shiftyjelly.pocketcasts.sharing.ui.VerticalSharePage
import dev.shreyaspatil.capturable.controller.CaptureController
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal interface SharePodcastPageListener {
    suspend fun onShare(podcast: Podcast, platform: SocialPlatform, cardType: CardType): SharingResponse
    fun onClose()

    companion object {
        val Preview = object : SharePodcastPageListener {
            override suspend fun onShare(podcast: Podcast, platform: SocialPlatform, cardType: CardType) = SharingResponse(
                isSuccsessful = true,
                feedbackMessage = null,
                error = null,
            )
            override fun onClose() = Unit
        }
    }
}

@Composable
internal fun SharePodcastPage(
    podcast: Podcast?,
    episodeCount: Int,
    socialPlatforms: Set<SocialPlatform>,
    shareColors: ShareColors,
    listener: SharePodcastPageListener,
    captureController: CaptureController,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    when (LocalConfiguration.current.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> HorizontalSharePodcastPage(
            podcast = podcast,
            episodeCount = episodeCount,
            socialPlatforms = socialPlatforms,
            shareColors = shareColors,
            listener = listener,
            snackbarHostState = snackbarHostState,
        )
        else -> VerticalSharePodcastPage(
            podcast = podcast,
            episodeCount = episodeCount,
            socialPlatforms = socialPlatforms,
            shareColors = shareColors,
            listener = listener,
            snackbarHostState = snackbarHostState,
            captureController = captureController,
        )
    }
}

@Composable
private fun VerticalSharePodcastPage(
    podcast: Podcast?,
    episodeCount: Int,
    socialPlatforms: Set<SocialPlatform>,
    shareColors: ShareColors,
    listener: SharePodcastPageListener,
    snackbarHostState: SnackbarHostState,
    captureController: CaptureController,
) {
    val scope = rememberCoroutineScope()
    VerticalSharePage(
        shareTitle = stringResource(LR.string.share_podcast_title),
        shareDescription = stringResource(LR.string.share_podcast_description),
        shareColors = shareColors,
        socialPlatforms = socialPlatforms,
        onClose = listener::onClose,
        snackbarHostState = snackbarHostState,
        onShareToPlatform = { platform, cardType ->
            if (podcast != null) {
                scope.launch {
                    val response = listener.onShare(podcast, platform, cardType)
                    response.feedbackMessage?.let { snackbarHostState.showSnackbar(it) }
                }
            }
        },
        middleContent = { cardType, modifier ->
            if (podcast != null) {
                when (cardType) {
                    CardType.Vertical -> VerticalPodcastCast(
                        podcast = podcast,
                        episodeCount = episodeCount,
                        shareColors = shareColors,
                        captureController = captureController,
                        modifier = modifier,
                    )
                    CardType.Horiozntal -> HorizontalPodcastCast(
                        podcast = podcast,
                        episodeCount = episodeCount,
                        shareColors = shareColors,
                        modifier = modifier,
                    )
                    CardType.Square -> SquarePodcastCast(
                        podcast = podcast,
                        episodeCount = episodeCount,
                        shareColors = shareColors,
                        modifier = modifier,
                    )
                }
            }
        },
    )
}

@Composable
private fun HorizontalSharePodcastPage(
    podcast: Podcast?,
    episodeCount: Int,
    socialPlatforms: Set<SocialPlatform>,
    shareColors: ShareColors,
    listener: SharePodcastPageListener,
    snackbarHostState: SnackbarHostState,
) {
    val scope = rememberCoroutineScope()
    HorizontalSharePage(
        shareTitle = stringResource(LR.string.share_podcast_title),
        shareDescription = stringResource(LR.string.share_podcast_description),
        shareColors = shareColors,
        socialPlatforms = socialPlatforms,
        snackbarHostState = snackbarHostState,
        onClose = listener::onClose,
        onShareToPlatform = { platform, cardType ->
            if (podcast != null) {
                scope.launch {
                    val response = listener.onShare(podcast, platform, cardType)
                    response.feedbackMessage?.let { snackbarHostState.showSnackbar(it) }
                }
            }
        },
        middleContent = {
            if (podcast != null) {
                HorizontalPodcastCast(
                    podcast = podcast,
                    episodeCount = episodeCount,
                    shareColors = shareColors,
                )
            }
        },
    )
}

@Preview(name = "SharePodcastVerticalRegularPreview", device = Devices.PortraitRegular)
@Composable
private fun SharePodcastVerticalRegularPreview() = SharePodcastPagePreview()

@Preview(name = "SharePodcastVerticalSmallPreview", device = Devices.PortraitSmall)
@Composable
private fun SharePodcastVerticalSmallPreviewPreview() = SharePodcastPagePreview()

@Preview(name = "SharePodcastVerticalTabletPreview", device = Devices.PortraitTablet)
@Composable
private fun SharePodcastVerticalTabletPreview() = SharePodcastPagePreview()

@Preview(name = "SharePodcastHorizontalRegularPreview", device = Devices.LandscapeRegular)
@Composable
private fun SharePodcastHorizontalRegularPreview() = SharePodcastPagePreview()

@Preview(name = "SharePodcastHorizontalSmallPreview", device = Devices.LandscapeSmall)
@Composable
private fun SharePodcastHorizontalSmallPreviewPreview() = SharePodcastPagePreview()

@Preview(name = "SharePodcastHorizontalTabletPreview", device = Devices.LandscapeTablet)
@Composable
private fun SharePodcastHorizontalTabletPreview() = SharePodcastPagePreview()

@Composable
private fun SharePodcastPagePreview(
    color: Long = 0xFFEC0404,
) = SharePodcastPage(
    podcast = Podcast(
        uuid = "podcast-id",
        title = "Podcast title",
        episodeFrequency = "monthly",
    ),
    episodeCount = 120,
    socialPlatforms = SocialPlatform.entries.toSet(),
    shareColors = ShareColors(Color(color)),
    listener = SharePodcastPageListener.Preview,
    captureController = rememberCaptureController(),
)
