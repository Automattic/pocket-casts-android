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
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharing.SharingResponse
import au.com.shiftyjelly.pocketcasts.sharing.social.SocialPlatform
import au.com.shiftyjelly.pocketcasts.sharing.ui.CardType
import au.com.shiftyjelly.pocketcasts.sharing.ui.Devices
import au.com.shiftyjelly.pocketcasts.sharing.ui.HorizontalEpisodeCard
import au.com.shiftyjelly.pocketcasts.sharing.ui.HorizontalSharePage
import au.com.shiftyjelly.pocketcasts.sharing.ui.ShareColors
import au.com.shiftyjelly.pocketcasts.sharing.ui.SquareEpisodeCard
import au.com.shiftyjelly.pocketcasts.sharing.ui.VerticalEpisodeCard
import au.com.shiftyjelly.pocketcasts.sharing.ui.VerticalSharePage
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import java.sql.Date
import java.time.Instant
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal interface ShareEpisodeTimestampPageListener {
    suspend fun onShare(podcast: Podcast, episode: PodcastEpisode, timestamp: Duration, platform: SocialPlatform, cardType: CardType): SharingResponse
    fun onClose()

    companion object {
        val Preview = object : ShareEpisodeTimestampPageListener {
            override suspend fun onShare(podcast: Podcast, episode: PodcastEpisode, timestamp: Duration, platform: SocialPlatform, cardType: CardType) = SharingResponse(
                isSuccsessful = true,
                feedbackMessage = null,
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
        middleContent = { cardType, modifier ->
            if (podcast != null && episode != null) {
                when (cardType) {
                    CardType.Vertical -> VerticalEpisodeCard(
                        podcast = podcast,
                        episode = episode,
                        useEpisodeArtwork = useEpisodeArtwork,
                        shareColors = shareColors,
                        modifier = modifier,
                    )
                    CardType.Horiozntal -> HorizontalEpisodeCard(
                        podcast = podcast,
                        episode = episode,
                        useEpisodeArtwork = useEpisodeArtwork,
                        shareColors = shareColors,
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
private fun HorizontalShareEpisodeTimestampPage(
    podcast: Podcast?,
    episode: PodcastEpisode?,
    timestamp: Duration,
    useEpisodeArtwork: Boolean,
    socialPlatforms: Set<SocialPlatform>,
    shareColors: ShareColors,
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
                )
            }
        },
    )
}

private fun Duration.toHhMmSs() = toComponents { hours, minutes, seconds, _ ->
    if (hours == 0L) {
        String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
    }
}

@ShowkaseComposable(name = "ShareEpisodeTimestampVerticalRegularPreview", group = "Sharing")
@Preview(name = "ShareEpisodeTimestampVerticalRegularPreview", device = Devices.PortraitRegular)
@Composable
fun ShareEpisodeTimestampVerticalRegularPreview() = ShareEpisodeTimestampPagePreview()

@ShowkaseComposable(name = "ShareEpisodeTimestampVerticalSmallPreview", group = "Sharing")
@Preview(name = "ShareEpisodeTimestampVerticalSmallPreview", device = Devices.PortraitSmall)
@Composable
fun ShareEpisodeTimestampVerticalSmallPreviewPreview() = ShareEpisodeTimestampPagePreview()

@ShowkaseComposable(name = "ShareEpisodeTimestampVerticalTabletPreview", group = "Sharing")
@Preview(name = "ShareEpisodeTimestampVerticalTabletPreview", device = Devices.PortraitTablet)
@Composable
fun ShareEpisodeTimestampVerticalTabletPreview() = ShareEpisodeTimestampPagePreview()

@ShowkaseComposable(name = "ShareEpisodeTimestampHorizontalRegularPreview", group = "Sharing")
@Preview(name = "ShareEpisodeTimestampHorizontalRegularPreview", device = Devices.LandscapeRegular)
@Composable
fun ShareEpisodeTimestampHorizontalRegularPreview() = ShareEpisodeTimestampPagePreview()

@ShowkaseComposable(name = "ShareEpisodeTimestampHorizontalSmallPreview", group = "Sharing")
@Preview(name = "ShareEpisodeTimestampHorizontalSmallPreview", device = Devices.LandscapeSmall)
@Composable
fun ShareEpisodeTimestampHorizontalSmallPreviewPreview() = ShareEpisodeTimestampPagePreview()

@ShowkaseComposable(name = "ShareEpisodeTimestampHorizontalTabletPreview", group = "Sharing")
@Preview(name = "ShareEpisodeTimestampHorizontalTabletPreview", device = Devices.LandscapeTablet)
@Composable
fun ShareEpisodeTimestampHorizontalTabletPreview() = ShareEpisodeTimestampPagePreview()

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
    listener = ShareEpisodeTimestampPageListener.Preview,
)
