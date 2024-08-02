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
            listener = listener,
            snackbarHostState = snackbarHostState,
        )
        else -> VerticalShareEpisodePage(
            podcast = podcast,
            episode = episode,
            useEpisodeArtwork = useEpisodeArtwork,
            socialPlatforms = socialPlatforms,
            shareColors = shareColors,
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
private fun HorizontalShareEpisodePage(
    podcast: Podcast?,
    episode: PodcastEpisode?,
    useEpisodeArtwork: Boolean,
    socialPlatforms: Set<SocialPlatform>,
    shareColors: ShareColors,
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
                )
            }
        },
    )
}

@ShowkaseComposable(name = "ShareEpisodeVerticalRegularPreview", group = "Sharing")
@Preview(name = "ShareEpisodeVerticalRegularPreview", device = Devices.PortraitRegular)
@Composable
fun ShareEpisodeVerticalRegularPreview() = ShareEpisodePagePreview()

@ShowkaseComposable(name = "ShareEpisodeVerticalSmallPreview", group = "Sharing")
@Preview(name = "ShareEpisodeVerticalSmallPreview", device = Devices.PortraitSmall)
@Composable
fun ShareEpisodeVerticalSmallPreviewPreview() = ShareEpisodePagePreview()

@ShowkaseComposable(name = "ShareEpisodeVerticalTabletPreview", group = "Sharing")
@Preview(name = "ShareEpisodeVerticalTabletPreview", device = Devices.PortraitTablet)
@Composable
fun ShareEpisodeVerticalTabletPreview() = ShareEpisodePagePreview()

@ShowkaseComposable(name = "ShareEpisodeHorizontalRegularPreview", group = "Sharing")
@Preview(name = "ShareEpisodeHorizontalRegularPreview", device = Devices.LandscapeRegular)
@Composable
fun ShareEpisodeHorizontalRegularPreview() = ShareEpisodePagePreview()

@ShowkaseComposable(name = "ShareEpisodeHorizontalSmallPreview", group = "Sharing")
@Preview(name = "ShareEpisodeHorizontalSmallPreview", device = Devices.LandscapeSmall)
@Composable
fun ShareEpisodeHorizontalSmallPreviewPreview() = ShareEpisodePagePreview()

@ShowkaseComposable(name = "ShareEpisodeHorizontalTabletPreview", group = "Sharing")
@Preview(name = "ShareEpisodeHorizontalTabletPreview", device = Devices.LandscapeTablet)
@Composable
fun ShareEpisodeHorizontalTabletPreview() = ShareEpisodePagePreview()

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
    listener = ShareEpisodePageListener.Preview,
)
