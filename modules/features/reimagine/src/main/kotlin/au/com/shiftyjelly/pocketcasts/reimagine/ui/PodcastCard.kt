package au.com.shiftyjelly.pocketcasts.reimagine.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.sharing.CardType
import au.com.shiftyjelly.pocketcasts.sharing.VisualCardType
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
    constrainedSize: (maxWidth: Dp, maxHeight: Dp) -> DpSize = { width, height -> DpSize(width, height) },
) = when (cardType) {
    CardType.Vertical -> VerticalPodcastCard(
        podcast = podcast,
        episodeCount = episodeCount,
        shareColors = shareColors,
        captureController = captureController,
        useHeightForAspectRatio = useHeightForAspectRatio,
        constrainedSize = constrainedSize,
        modifier = modifier,
    )
    CardType.Horizontal -> HorizontalPodcastCard(
        podcast = podcast,
        episodeCount = episodeCount,
        shareColors = shareColors,
        captureController = captureController,
        useHeightForAspectRatio = useHeightForAspectRatio,
        constrainedSize = constrainedSize,
        modifier = modifier,
    )
    CardType.Square -> SquarePodcastCard(
        podcast = podcast,
        episodeCount = episodeCount,
        shareColors = shareColors,
        captureController = captureController,
        constrainedSize = constrainedSize,
        modifier = modifier,
    )
}
