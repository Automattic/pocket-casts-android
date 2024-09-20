package au.com.shiftyjelly.pocketcasts.reimagine.podcast

import android.content.res.Configuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.reimagine.ui.BackgroundAssetController
import au.com.shiftyjelly.pocketcasts.reimagine.ui.HorizontalPodcastCard
import au.com.shiftyjelly.pocketcasts.reimagine.ui.HorizontalSharePage
import au.com.shiftyjelly.pocketcasts.reimagine.ui.PodcastCard
import au.com.shiftyjelly.pocketcasts.reimagine.ui.ShareColors
import au.com.shiftyjelly.pocketcasts.reimagine.ui.VerticalSharePage
import au.com.shiftyjelly.pocketcasts.sharing.CardType
import au.com.shiftyjelly.pocketcasts.sharing.SharingResponse
import au.com.shiftyjelly.pocketcasts.sharing.SocialPlatform
import au.com.shiftyjelly.pocketcasts.sharing.VisualCardType
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal interface SharePodcastPageListener {
    suspend fun onShare(podcast: Podcast, platform: SocialPlatform, cardType: VisualCardType): SharingResponse
    fun onClose()

    companion object {
        val Preview = object : SharePodcastPageListener {
            override suspend fun onShare(podcast: Podcast, platform: SocialPlatform, cardType: VisualCardType) = SharingResponse(
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
    assetController: BackgroundAssetController,
    listener: SharePodcastPageListener,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    when (LocalConfiguration.current.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> HorizontalSharePodcastPage(
            podcast = podcast,
            episodeCount = episodeCount,
            socialPlatforms = socialPlatforms,
            shareColors = shareColors,
            assetController = assetController,
            listener = listener,
            snackbarHostState = snackbarHostState,
        )
        else -> VerticalSharePodcastPage(
            podcast = podcast,
            episodeCount = episodeCount,
            socialPlatforms = socialPlatforms,
            shareColors = shareColors,
            assetController = assetController,
            listener = listener,
            snackbarHostState = snackbarHostState,
        )
    }
}

@Composable
private fun VerticalSharePodcastPage(
    podcast: Podcast?,
    episodeCount: Int,
    socialPlatforms: Set<SocialPlatform>,
    shareColors: ShareColors,
    assetController: BackgroundAssetController,
    listener: SharePodcastPageListener,
    snackbarHostState: SnackbarHostState,
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
        middleContent = { cardType, cardSize, modifier ->
            if (podcast != null) {
                val captureController = assetController.captureController(cardType)
                PodcastCard(
                    cardType = cardType,
                    podcast = podcast,
                    episodeCount = episodeCount,
                    shareColors = shareColors,
                    captureController = captureController,
                    constrainedSize = { _, _ -> cardSize },
                    modifier = modifier,
                )
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
    assetController: BackgroundAssetController,
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
                HorizontalPodcastCard(
                    podcast = podcast,
                    episodeCount = episodeCount,
                    shareColors = shareColors,
                    constrainedSize = { maxWidth, maxHeight -> DpSize(maxWidth.coerceAtMost(400.dp), maxHeight) },
                    captureController = assetController.captureController(CardType.Horizontal),
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
    assetController = BackgroundAssetController.preview(),
    listener = SharePodcastPageListener.Preview,
)
