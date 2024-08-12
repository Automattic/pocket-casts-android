package au.com.shiftyjelly.pocketcasts.sharing.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import dev.shreyaspatil.capturable.controller.CaptureController

@Composable
internal fun EpisodeCard(
    cardType: VisualCardType,
    episode: PodcastEpisode,
    podcast: Podcast,
    useEpisodeArtwork: Boolean,
    shareColors: ShareColors,
    captureController: CaptureController,
    modifier: Modifier = Modifier,
    useHeightForAspectRatio: Boolean = cardType == CardType.Vertical,
    customSize: DpSize? = null,
) = when (cardType) {
    CardType.Vertical -> VerticalEpisodeCard(
        episode = episode,
        podcast = podcast,
        useEpisodeArtwork = useEpisodeArtwork,
        shareColors = shareColors,
        captureController = captureController,
        useHeightForAspectRatio = useHeightForAspectRatio,
        customSize = customSize,
        modifier = modifier,
    )
    CardType.Horizontal -> HorizontalEpisodeCard(
        episode = episode,
        podcast = podcast,
        useEpisodeArtwork = useEpisodeArtwork,
        shareColors = shareColors,
        captureController = captureController,
        useHeightForAspectRatio = useHeightForAspectRatio,
        customSize = customSize,
        modifier = modifier,
    )
    CardType.Square -> SquareEpisodeCard(
        episode = episode,
        podcast = podcast,
        useEpisodeArtwork = useEpisodeArtwork,
        shareColors = shareColors,
        captureController = captureController,
        customSize = customSize,
        modifier = modifier,
    )
}
