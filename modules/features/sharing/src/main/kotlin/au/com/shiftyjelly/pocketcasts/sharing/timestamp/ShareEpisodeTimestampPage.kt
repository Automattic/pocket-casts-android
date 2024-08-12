package au.com.shiftyjelly.pocketcasts.sharing.timestamp

import android.content.res.Configuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharing.SharingResponse
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform
import au.com.shiftyjelly.pocketcasts.sharing.ui.BackgroundAssetController
import au.com.shiftyjelly.pocketcasts.sharing.ui.CardType
import au.com.shiftyjelly.pocketcasts.sharing.ui.EpisodeCard
import au.com.shiftyjelly.pocketcasts.sharing.ui.HorizontalEpisodeCard
import au.com.shiftyjelly.pocketcasts.sharing.ui.HorizontalSharePage
import au.com.shiftyjelly.pocketcasts.sharing.ui.ShareColors
import au.com.shiftyjelly.pocketcasts.sharing.ui.VerticalSharePage
import au.com.shiftyjelly.pocketcasts.sharing.ui.VisualCardType
import au.com.shiftyjelly.pocketcasts.utils.toHhMmSs
import java.sql.Date
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal interface ShareEpisodeTimestampPageListener {
    suspend fun onShare(podcast: Podcast, episode: PodcastEpisode, timestamp: Duration, platform: SocialPlatform, cardType: VisualCardType): SharingResponse
    fun onClose()

    companion object {
        val Preview = object : ShareEpisodeTimestampPageListener {
            override suspend fun onShare(podcast: Podcast, episode: PodcastEpisode, timestamp: Duration, platform: SocialPlatform, cardType: VisualCardType) = SharingResponse(
                isSuccsessful = true,
                feedbackMessage = null,
                error = null,
            )
            override fun onClose() = Unit
        }
    }
}

@Composable
internal fun ShareEpisodeTimestampPage(
    podcast: Podcast?,
    episode: PodcastEpisode?,
    timestamp: Duration,
    useEpisodeArtwork: Boolean,
    socialPlatforms: Set<SocialPlatform>,
    shareColors: ShareColors,
    assetController: BackgroundAssetController,
    listener: ShareEpisodeTimestampPageListener,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    when (LocalConfiguration.current.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> HorizontalShareEpisodeTimestampPage(
            podcast = podcast,
            episode = episode,
            timestamp = timestamp,
            useEpisodeArtwork = useEpisodeArtwork,
            socialPlatforms = socialPlatforms,
            shareColors = shareColors,
            assetController = assetController,
            listener = listener,
            snackbarHostState = snackbarHostState,
        )
        else -> VerticalShareEpisodeTimestampPage(
            podcast = podcast,
            episode = episode,
            timestamp = timestamp,
            useEpisodeArtwork = useEpisodeArtwork,
            socialPlatforms = socialPlatforms,
            shareColors = shareColors,
            assetController = assetController,
            listener = listener,
            snackbarHostState = snackbarHostState,
        )
    }
}

@Composable
private fun VerticalShareEpisodeTimestampPage(
    podcast: Podcast?,
    episode: PodcastEpisode?,
    useEpisodeArtwork: Boolean,
    timestamp: Duration,
    socialPlatforms: Set<SocialPlatform>,
    shareColors: ShareColors,
    assetController: BackgroundAssetController,
    listener: ShareEpisodeTimestampPageListener,
    snackbarHostState: SnackbarHostState,
) {
    val scope = rememberCoroutineScope()
    VerticalSharePage(
        shareTitle = stringResource(LR.string.share_episode_timestamp_title, timestamp.toHhMmSs()),
        shareDescription = stringResource(LR.string.share_episode_timestamp_description),
        shareColors = shareColors,
        socialPlatforms = socialPlatforms,
        snackbarHostState = snackbarHostState,
        onClose = listener::onClose,
        onShareToPlatform = { platform, cardType ->
            if (podcast != null && episode != null) {
                scope.launch {
                    val response = listener.onShare(podcast, episode, timestamp, platform, cardType)
                    response.feedbackMessage?.let { snackbarHostState.showSnackbar(it) }
                }
            }
        },
        middleContent = { cardType, cardSize, modifier ->
            if (podcast != null && episode != null) {
                val captureController = assetController.captureController(cardType)
                EpisodeCard(
                    cardType = cardType,
                    podcast = podcast,
                    episode = episode,
                    useEpisodeArtwork = useEpisodeArtwork,
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
private fun HorizontalShareEpisodeTimestampPage(
    podcast: Podcast?,
    episode: PodcastEpisode?,
    timestamp: Duration,
    useEpisodeArtwork: Boolean,
    socialPlatforms: Set<SocialPlatform>,
    shareColors: ShareColors,
    assetController: BackgroundAssetController,
    listener: ShareEpisodeTimestampPageListener,
    snackbarHostState: SnackbarHostState,
) {
    val scope = rememberCoroutineScope()
    HorizontalSharePage(
        shareTitle = stringResource(LR.string.share_episode_timestamp_title, timestamp.toHhMmSs()),
        shareDescription = stringResource(LR.string.share_episode_timestamp_description),
        shareColors = shareColors,
        socialPlatforms = socialPlatforms,
        snackbarHostState = snackbarHostState,
        onClose = listener::onClose,
        onShareToPlatform = { platform, cardType ->
            if (podcast != null && episode != null) {
                scope.launch {
                    val response = listener.onShare(podcast, episode, timestamp, platform, cardType)
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
                    captureController = assetController.captureController(CardType.Horizontal),
                )
            }
        },
    )
}

@Preview(name = "ShareEpisodeTimestampVerticalRegularPreview", device = Devices.PortraitRegular)
@Composable
private fun ShareEpisodeTimestampVerticalRegularPreview() = ShareEpisodeTimestampPagePreview()

@Preview(name = "ShareEpisodeTimestampVerticalSmallPreview", device = Devices.PortraitSmall)
@Composable
private fun ShareEpisodeTimestampVerticalSmallPreviewPreview() = ShareEpisodeTimestampPagePreview()

@Preview(name = "ShareEpisodeTimestampVerticalTabletPreview", device = Devices.PortraitTablet)
@Composable
private fun ShareEpisodeTimestampVerticalTabletPreview() = ShareEpisodeTimestampPagePreview()

@Preview(name = "ShareEpisodeTimestampHorizontalRegularPreview", device = Devices.LandscapeRegular)
@Composable
private fun ShareEpisodeTimestampHorizontalRegularPreview() = ShareEpisodeTimestampPagePreview()

@Preview(name = "ShareEpisodeTimestampHorizontalSmallPreview", device = Devices.LandscapeSmall)
@Composable
private fun ShareEpisodeTimestampHorizontalSmallPreviewPreview() = ShareEpisodeTimestampPagePreview()

@Preview(name = "ShareEpisodeTimestampHorizontalTabletPreview", device = Devices.LandscapeTablet)
@Composable
private fun ShareEpisodeTimestampHorizontalTabletPreview() = ShareEpisodeTimestampPagePreview()

@Composable
private fun ShareEpisodeTimestampPagePreview(
    color: Long = 0xFFEC0404,
) = ShareEpisodeTimestampPage(
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
    timestamp = 23.minutes + 11.seconds,
    useEpisodeArtwork = false,
    socialPlatforms = SocialPlatform.entries.toSet(),
    shareColors = ShareColors(Color(color)),
    assetController = BackgroundAssetController.preview(),
    listener = ShareEpisodeTimestampPageListener.Preview,
)
