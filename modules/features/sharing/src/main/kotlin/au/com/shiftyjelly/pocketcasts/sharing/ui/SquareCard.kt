package au.com.shiftyjelly.pocketcasts.sharing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import java.sql.Date
import java.time.Instant

@Composable
internal fun SquarePodcastCast(
    podcast: Podcast,
    episodeCount: Int,
    shareColors: ShareColors,
    modifier: Modifier = Modifier,
) = SquareCard(
    data = PodcastCardData(
        podcast = podcast,
        episodeCount = episodeCount,
    ),
    shareColors = shareColors,
    modifier = modifier,
)

@Composable
internal fun SquareEpisodeCard(
    episode: PodcastEpisode,
    podcast: Podcast,
    useEpisodeArtwork: Boolean,
    shareColors: ShareColors,
    modifier: Modifier = Modifier,
) = SquareCard(
    data = EpisodeCardData(
        episode = episode,
        podcast = podcast,
        useEpisodeArtwork = useEpisodeArtwork,
    ),
    shareColors = shareColors,
    modifier = modifier,
)

@Composable
private fun SquareCard(
    data: CardData,
    shareColors: ShareColors,
    modifier: Modifier = Modifier,
) = BoxWithConstraints {
    val size = minOf(maxWidth, maxHeight)
    val backgroundGradient = Brush.verticalGradient(
        listOf(
            shareColors.cardTop,
            shareColors.cardBottom,
        ),
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(backgroundGradient, RoundedCornerShape(12.dp))
            .size(size),
    ) {
        Spacer(
            modifier = Modifier.height(42.dp),
        )
        data.Image(
            modifier = Modifier
                .size(size * 0.4f)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(
            modifier = Modifier.height(24.dp),
        )
        TextH70(
            text = data.topText(),
            maxLines = 1,
            color = shareColors.cardText.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 64.dp),
        )
        Spacer(
            modifier = Modifier.height(6.dp),
        )
        TextH40(
            text = data.middleText(),
            maxLines = 2,
            textAlign = TextAlign.Center,
            color = shareColors.cardText,
            modifier = Modifier.padding(horizontal = 42.dp),
        )
        Spacer(
            modifier = Modifier.height(6.dp),
        )
        TextH70(
            text = data.bottomText(),
            maxLines = 2,
            textAlign = TextAlign.Center,
            color = shareColors.cardText.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 64.dp),
        )
    }
}

@ShowkaseComposable(name = "SquarePodcastCard", group = "Sharing", styleName = "Light")
@Preview(name = "SquarePodcastCardLight")
@Composable
fun SquarePodcastCardLightPreview() = SquarePodcastCardPreview(
    baseColor = Color(0xFFFBCB04),
)

@ShowkaseComposable(name = "SquarePodcastCard", group = "Sharing", styleName = "Dark")
@Preview(name = "SquarePodcastCardDark")
@Composable
fun SquarePodcastCardDarkPreview() = SquarePodcastCardPreview(
    baseColor = Color(0xFFEC0404),
)

@ShowkaseComposable(name = "SquareEpisodeCard", group = "Sharing", styleName = "Light")
@Preview(name = "SquareEpisodeCardLight")
@Composable
fun SquareEpisodeCardLightPreview() = SquareEpisodeCardPreview(
    baseColor = Color(0xFFFBCB04),
)

@ShowkaseComposable(name = "SquareEpisodeCard", group = "Sharing", styleName = "Dark")
@Preview(name = "SquareEpisodeCardDark")
@Composable
fun SquareEpisodeCardDarkPreview() = SquareEpisodeCardPreview(
    baseColor = Color(0xFFEC0404),
)

@Composable
private fun SquarePodcastCardPreview(
    baseColor: Color,
) = SquareCardPreview(
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
private fun SquareEpisodeCardPreview(
    baseColor: Color,
) = SquareCardPreview(
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
private fun SquareCardPreview(
    data: CardData,
    baseColor: Color,
) = SquareCard(
    data = data,
    shareColors = ShareColors(baseColor),
)
