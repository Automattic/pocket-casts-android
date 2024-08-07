package au.com.shiftyjelly.pocketcasts.sharing.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import dev.shreyaspatil.capturable.controller.CaptureController

@Composable
internal fun PodcastCard(
    cardType: VisualCardType,
    podcast: Podcast,
    episodeCount: Int,
    shareColors: ShareColors,
    captureController: CaptureController,
    modifier: Modifier = Modifier,
    useHeightForAspectRatio: Boolean = cardType == CardType.Vertical,
    customSize: DpSize? = null,
) = when (cardType) {
    CardType.Vertical -> VerticalPodcastCast(
        podcast = podcast,
        episodeCount = episodeCount,
        shareColors = shareColors,
        captureController = captureController,
        useHeightForAspectRatio = useHeightForAspectRatio,
        customSize = customSize,
        modifier = modifier,
    )
    CardType.Horizontal -> HorizontalPodcastCast(
        podcast = podcast,
        episodeCount = episodeCount,
        shareColors = shareColors,
        captureController = captureController,
        useHeightForAspectRatio = useHeightForAspectRatio,
        customSize = customSize,
        modifier = modifier,
    )
    CardType.Square -> SquarePodcastCast(
        podcast = podcast,
        episodeCount = episodeCount,
        shareColors = shareColors,
        captureController = captureController,
        customSize = customSize,
        modifier = modifier,
    )
}
