package au.com.shiftyjelly.pocketcasts.reimagine.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.PocketCastsLogo
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharing.CardType
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.CaptureController
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import java.sql.Date
import java.time.Instant

@Composable
internal fun HorizontalPodcastCard(
    podcast: Podcast,
    episodeCount: Int,
    shareColors: ShareColors,
    captureController: CaptureController,
    modifier: Modifier = Modifier,
    useHeightForAspectRatio: Boolean = false,
    constrainedSize: (maxWidth: Dp, maxHeight: Dp) -> DpSize = { width, height -> DpSize(width, height) },
) = HorizontalCard(
    data = PodcastCardData(
        podcast = podcast,
        episodeCount = episodeCount,
    ),
    shareColors = shareColors,
    useHeightForAspectRatio = useHeightForAspectRatio,
    constrainedSize = constrainedSize,
    captureController = captureController,
    modifier = modifier,
)

@Composable
internal fun HorizontalEpisodeCard(
    episode: PodcastEpisode,
    podcast: Podcast,
    useEpisodeArtwork: Boolean,
    shareColors: ShareColors,
    captureController: CaptureController,
    modifier: Modifier = Modifier,
    useHeightForAspectRatio: Boolean = false,
    constrainedSize: (maxWidth: Dp, maxHeight: Dp) -> DpSize = { width, height -> DpSize(width, height) },
) = HorizontalCard(
    data = EpisodeCardData(
        episode = episode,
        podcast = podcast,
        useEpisodeArtwork = useEpisodeArtwork,
    ),
    shareColors = shareColors,
    useHeightForAspectRatio = useHeightForAspectRatio,
    constrainedSize = constrainedSize,
    captureController = captureController,
    modifier = modifier,
)

@ShowkaseComposable(name = "Horizontal podcast card", group = "Sharing")
@Preview(name = "HorizontalPodcastCardDark")
@Composable
fun HorizontalPodcastCardDarkPreview() = HorizontalPodcastCardPreview(
    baseColor = Color(0xFFEC0404),
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun HorizontalCard(
    data: CardData,
    shareColors: ShareColors,
    useHeightForAspectRatio: Boolean,
    captureController: CaptureController,
    modifier: Modifier = Modifier,
    constrainedSize: (maxWidth: Dp, maxHeight: Dp) -> DpSize = { width, height -> DpSize(width, height) },
) = BoxWithConstraints(
    contentAlignment = Alignment.Center,
    modifier = modifier,
) {
    val backgroundGradient = Brush.verticalGradient(
        listOf(
            shareColors.cardTop,
            shareColors.cardBottom,
        ),
    )
    val size = constrainedSize(maxWidth, maxHeight)
    val (width, height) = if (useHeightForAspectRatio) {
        size.height / CardType.Horizontal.aspectRatio to size.height
    } else {
        size.width to size.width * CardType.Horizontal.aspectRatio
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .capturable(captureController)
            .background(backgroundGradient, RoundedCornerShape(12.dp))
            .width(width)
            .height(height),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(
                modifier = Modifier.width(height * 0.15f),
            )
            data.Image(
                modifier = Modifier
                    .size(height * 0.7f)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(
                modifier = Modifier.width(height * 0.10f),
            )
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(end = height * 0.15f),
            ) {
                TextH70(
                    text = data.topText(),
                    disableScale = true,
                    color = shareColors.cardTextSecondary,
                    maxLines = 1,
                    modifier = Modifier.padding(end = height * 0.08f),
                )
                Spacer(
                    modifier = Modifier.height(6.dp),
                )
                TextH40(
                    text = data.middleText(),
                    disableScale = true,
                    color = shareColors.cardTextPrimary,
                    maxLines = 3,
                )
                Spacer(
                    modifier = Modifier.height(6.dp),
                )
                TextH70(
                    text = data.bottomText(),
                    disableScale = true,
                    maxLines = 2,
                    color = shareColors.cardTextSecondary,
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = height * 0.08f, end = height * 0.08f),
        ) {
            PocketCastsLogo(
                modifier = Modifier.size(height * 0.15f),
            )
        }
    }
}

@Preview(name = "HorizontalPodcastCardLight")
@Composable
private fun HorizontalPodcastCardLightPreview() = HorizontalPodcastCardPreview(
    baseColor = Color(0xFFFBCB04),
)

@ShowkaseComposable(name = "Horizontal episode card", group = "Sharing")
@Preview(name = "HorizontalEpisodeCardDark")
@Composable
fun HorizontalEpisodeCardDarkPreview() = HorizontalEpisodeCardPreview(
    baseColor = Color(0xFFEC0404),
)

@Preview(name = "HorizontalEpisodeCardLight")
@Composable
private fun HorizontalEpisodeCardLightPreview() = HorizontalEpisodeCardPreview(
    baseColor = Color(0xFFFBCB04),
)

@Composable
private fun HorizontalPodcastCardPreview(
    baseColor: Color,
) = HorizontalCardPreview(
    data = PodcastCardData(
        podcast = Podcast(
            uuid = "podcast-id",
            title = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
            episodeFrequency = "monthly",
        ),
        episodeCount = 120,
    ),
    baseColor = baseColor,
)

@Composable
private fun HorizontalEpisodeCardPreview(
    baseColor: Color,
) = HorizontalCardPreview(
    data = EpisodeCardData(
        episode = PodcastEpisode(
            uuid = "episode-id",
            podcastUuid = "podcast-id",
            publishedDate = Date.from(Instant.parse("2024-12-03T10:15:30.00Z")),
            title = "Nobis sapiente fugit vitae. Iusto magnam nam nam et odio. Debitis cupiditate officiis et. Sit quia in voluptate sit voluptatem magni.",
        ),
        podcast = Podcast(
            uuid = "podcast-id",
            title = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
        ),
        useEpisodeArtwork = true,
    ),
    baseColor = baseColor,
)

@Composable
private fun HorizontalCardPreview(
    data: CardData,
    baseColor: Color,
) = HorizontalCard(
    data = data,
    shareColors = ShareColors(baseColor),
    useHeightForAspectRatio = false,
    captureController = rememberCaptureController(),
)
