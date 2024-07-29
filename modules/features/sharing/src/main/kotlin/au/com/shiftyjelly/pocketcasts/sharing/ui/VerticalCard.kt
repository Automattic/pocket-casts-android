package au.com.shiftyjelly.pocketcasts.sharing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.PocketCastsPill
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import java.sql.Date
import java.time.Instant

@Composable
internal fun VerticalPodcastCast(
    podcast: Podcast,
    episodeCount: Int,
    shareColors: ShareColors,
    modifier: Modifier = Modifier,
    useHeightForAspectRatio: Boolean = true,
) = VerticalCard(
    data = PodcastCardData(
        podcast = podcast,
        episodeCount = episodeCount,
    ),
    shareColors = shareColors,
    useHeightForAspectRatio = useHeightForAspectRatio,
    modifier = modifier,
)

@Composable
internal fun VerticalEpisodeCard(
    episode: PodcastEpisode,
    podcast: Podcast,
    useEpisodeArtwork: Boolean,
    shareColors: ShareColors,
    modifier: Modifier = Modifier,
    useHeightForAspectRatio: Boolean = true,
) = VerticalCard(
    data = EpisodeCardData(
        episode = episode,
        podcast = podcast,
        useEpisodeArtwork = useEpisodeArtwork,
    ),
    shareColors = shareColors,
    useHeightForAspectRatio = useHeightForAspectRatio,
    modifier = modifier,
)

@Composable
private fun VerticalCard(
    data: CardData,
    shareColors: ShareColors,
    useHeightForAspectRatio: Boolean,
    modifier: Modifier = Modifier,
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
    val (height, width) = if (useHeightForAspectRatio) {
        maxHeight to maxHeight / 1.5f
    } else {
        maxWidth * 1.5f to maxWidth
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(backgroundGradient, RoundedCornerShape(12.dp))
            .width(width)
            .height(height),
    ) {
        Spacer(
            modifier = Modifier.height(width * 0.15f),
        )
        data.Image(
            modifier = Modifier
                .size(width * 0.7f)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(
            modifier = Modifier.height(height * 0.05f),
        )
        TextH70(
            text = data.topText(),
            maxLines = 1,
            color = shareColors.cardText.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = width * 0.1f),
        )
        Spacer(
            modifier = Modifier.height(6.dp),
        )
        TextH40(
            text = data.middleText(),
            maxLines = 2,
            textAlign = TextAlign.Center,
            color = shareColors.cardText,
            modifier = Modifier.padding(horizontal = width * 0.1f),
        )
        Spacer(
            modifier = Modifier.height(6.dp),
        )
        TextH70(
            text = data.bottomText(),
            maxLines = 2,
            textAlign = TextAlign.Center,
            color = shareColors.cardText.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = width * 0.1f),
        )
        Spacer(
            modifier = Modifier.weight(1f),
        )
        PocketCastsPill()
        Spacer(
            modifier = Modifier.weight(1f),
        )
    }
}

@ShowkaseComposable(name = "VerticalPodcastCard", group = "Sharing", styleName = "Light")
@Preview(name = "VerticalPodcastCardLight")
@Composable
fun VerticalPodcastCardLightPreview() = VerticalPodcastCardPreview(
    baseColor = Color(0xFFFBCB04),
)

@ShowkaseComposable(name = "VerticalPodcastCard", group = "Sharing", styleName = "Dark")
@Preview(name = "VerticalPodcastCardDark")
@Composable
fun VerticalPodcastCardDarkPreview() = VerticalPodcastCardPreview(
    baseColor = Color(0xFFEC0404),
)

@ShowkaseComposable(name = "VerticalEpisodeCard", group = "Sharing", styleName = "Light")
@Preview(name = "VerticalEpisodeCardLight")
@Composable
fun VerticalEpisodeCardLightPreview() = VerticalEpisodeCardPreview(
    baseColor = Color(0xFFFBCB04),
)

@ShowkaseComposable(name = "VerticalEpisodeCard", group = "Sharing", styleName = "Dark")
@Preview(name = "VerticalEpisodeCardDark")
@Composable
fun VerticalEpisodeCardDarkPreview() = VerticalEpisodeCardPreview(
    baseColor = Color(0xFFEC0404),
)

@Composable
private fun VerticalPodcastCardPreview(
    baseColor: Color,
) = VerticalCardPreview(
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
private fun VerticalEpisodeCardPreview(
    baseColor: Color,
) = VerticalCardPreview(
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
private fun VerticalCardPreview(
    data: CardData,
    baseColor: Color,
) = VerticalCard(
    data = data,
    shareColors = ShareColors(baseColor),
    useHeightForAspectRatio = false,
)
