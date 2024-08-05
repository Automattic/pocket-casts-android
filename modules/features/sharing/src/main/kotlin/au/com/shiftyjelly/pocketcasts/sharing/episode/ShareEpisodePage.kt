package au.com.shiftyjelly.pocketcasts.sharing.episode

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharing.SharingResponse
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform
import au.com.shiftyjelly.pocketcasts.sharing.ui.BackgroundAssetController
import au.com.shiftyjelly.pocketcasts.sharing.ui.CardType
import au.com.shiftyjelly.pocketcasts.sharing.ui.Devices
import au.com.shiftyjelly.pocketcasts.sharing.ui.HorizontalEpisodeCard
import au.com.shiftyjelly.pocketcasts.sharing.ui.HorizontalSharePage
import au.com.shiftyjelly.pocketcasts.sharing.ui.ShareColors
import au.com.shiftyjelly.pocketcasts.sharing.ui.SquareEpisodeCard
import au.com.shiftyjelly.pocketcasts.sharing.ui.VerticalEpisodeCard
import au.com.shiftyjelly.pocketcasts.sharing.ui.VerticalSharePage
import java.sql.Date
import java.time.Instant
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal interface ShareEpisodePageListener {
    suspend fun onShare(podcast: Podcast, episode: PodcastEpisode, platform: SocialPlatform, cardType: CardType): SharingResponse
    fun onClose()

    companion object {
        val Preview = object : ShareEpisodePageListener {
            override suspend fun onShare(podcast: Podcast, episode: PodcastEpisode, platform: SocialPlatform, cardType: CardType) = SharingResponse(
                isSuccsessful = true,
                feedbackMessage = null,
                error = null,
            )
            override fun onClose() = Unit
        }
    }
}

@Composable
internal fun ShareEpisodePage(
    podcast: Podcast?,
    episode: PodcastEpisode?,
    useEpisodeArtwork: Boolean,
    socialPlatforms: Set<SocialPlatform>,
    shareColors: ShareColors,
    assetController: BackgroundAssetController,
    listener: ShareEpisodePageListener,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    when (LocalConfiguration.current.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> HorizontalShareEpisodePage(
            podcast = podcast,
            episode = episode,
            useEpisodeArtwork = useEpisodeArtwork,
            socialPlatforms = socialPlatforms,
            shareColors = shareColors,
            assetController = assetController,
            listener = listener,
            snackbarHostState = snackbarHostState,
        )
        else -> VerticalShareEpisodePage(
            podcast = podcast,
            episode = episode,
            useEpisodeArtwork = useEpisodeArtwork,
            socialPlatforms = socialPlatforms,
            shareColors = shareColors,
            assetController = assetController,
            listener = listener,
            snackbarHostState = snackbarHostState,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VerticalShareEpisodePage(
    podcast: Podcast?,
    episode: PodcastEpisode?,
    useEpisodeArtwork: Boolean,
    socialPlatforms: Set<SocialPlatform>,
    shareColors: ShareColors,
    assetController: BackgroundAssetController,
    listener: ShareEpisodePageListener,
    snackbarHostState: SnackbarHostState,
) {
    val scope = rememberCoroutineScope()
    VerticalSharePage(
        shareTitle = stringResource(LR.string.share_episode_title),
        shareDescription = stringResource(LR.string.share_episode_description),
        shareColors = shareColors,
        socialPlatforms = socialPlatforms,
        snackbarHostState = snackbarHostState,
        onClose = listener::onClose,
        onShareToPlatform = { platform, cardType ->
            if (podcast != null && episode != null) {
                scope.launch {
                    val response = listener.onShare(podcast, episode, platform, cardType)
                    response.feedbackMessage?.let { snackbarHostState.showSnackbar(it) }
                }
            }
        },
        middleContent = { cardType, modifier ->
            if (podcast != null && episode != null) {
                val captureController = assetController.captureController(cardType)
                when (cardType) {
                    CardType.Vertical -> VerticalEpisodeCard(
                        podcast = podcast,
                        episode = episode,
                        useEpisodeArtwork = useEpisodeArtwork,
                        shareColors = shareColors,
                        captureController = captureController,
                        modifier = modifier,
                    )
                    CardType.Horiozntal -> HorizontalEpisodeCard(
                        podcast = podcast,
                        episode = episode,
                        useEpisodeArtwork = useEpisodeArtwork,
                        shareColors = shareColors,
                        captureController = captureController,
                        modifier = modifier,
                    )
                    CardType.Square -> SquareEpisodeCard(
                        podcast = podcast,
                        episode = episode,
                        useEpisodeArtwork = useEpisodeArtwork,
                        shareColors = shareColors,
                        modifier = modifier,
                    )
                }
            }
        },
    )
}

@Composable
private fun HorizontalShareEpisodePage(
    podcast: Podcast?,
    episode: PodcastEpisode?,
    useEpisodeArtwork: Boolean,
    socialPlatforms: Set<SocialPlatform>,
    shareColors: ShareColors,
    assetController: BackgroundAssetController,
    listener: ShareEpisodePageListener,
    snackbarHostState: SnackbarHostState,
) {
    val scope = rememberCoroutineScope()
    HorizontalSharePage(
        shareTitle = stringResource(LR.string.share_episode_title),
        shareDescription = stringResource(LR.string.share_episode_description),
        shareColors = shareColors,
        socialPlatforms = socialPlatforms,
        snackbarHostState = snackbarHostState,
        onClose = listener::onClose,
        onShareToPlatform = { platform, cardType ->
            if (podcast != null && episode != null) {
                scope.launch {
                    val response = listener.onShare(podcast, episode, platform, cardType)
                    response.feedbackMessage?.let { snackbarHostState.showSnackbar(it) }
                }
            }
        },
        middleContent = {
            if (podcast != null && episode != null) {
                HorizontalEpisodeCard(
                    podcast = podcast,
                    episode = episode,
                    useEpisodeArtwork = useEpisodeArtwork,
                    shareColors = shareColors,
                    captureController = assetController.captureController(CardType.Horiozntal),
                )
            }
        },
    )
}

@Preview(name = "ShareEpisodeVerticalRegularPreview", device = Devices.PortraitRegular)
@Composable
private fun ShareEpisodeVerticalRegularPreview() = ShareEpisodePagePreview()

@Preview(name = "ShareEpisodeVerticalSmallPreview", device = Devices.PortraitSmall)
@Composable
private fun ShareEpisodeVerticalSmallPreviewPreview() = ShareEpisodePagePreview()

@Preview(name = "ShareEpisodeVerticalTabletPreview", device = Devices.PortraitTablet)
@Composable
private fun ShareEpisodeVerticalTabletPreview() = ShareEpisodePagePreview()

@Preview(name = "ShareEpisodeHorizontalRegularPreview", device = Devices.LandscapeRegular)
@Composable
private fun ShareEpisodeHorizontalRegularPreview() = ShareEpisodePagePreview()

@Preview(name = "ShareEpisodeHorizontalSmallPreview", device = Devices.LandscapeSmall)
@Composable
private fun ShareEpisodeHorizontalSmallPreviewPreview() = ShareEpisodePagePreview()

@Preview(name = "ShareEpisodeHorizontalTabletPreview", device = Devices.LandscapeTablet)
@Composable
private fun ShareEpisodeHorizontalTabletPreview() = ShareEpisodePagePreview()

@Composable
private fun ShareEpisodePagePreview(
    color: Long = 0xFFEC0404,
) = ShareEpisodePage(
    podcast = Podcast(
        uuid = "podcast-id",
        title = "Podcast title",
    ),
    episode = PodcastEpisode(
        uuid = "episode-id",
        podcastUuid = "podcast-id",
        publishedDate = Date.from(Instant.parse("2024-12-03T10:15:30.00Z")),
        title = "Episode title",
    ),
    useEpisodeArtwork = false,
    socialPlatforms = SocialPlatform.entries.toSet(),
    shareColors = ShareColors(Color(color)),
    assetController = BackgroundAssetController.preview(),
    listener = ShareEpisodePageListener.Preview,
)
