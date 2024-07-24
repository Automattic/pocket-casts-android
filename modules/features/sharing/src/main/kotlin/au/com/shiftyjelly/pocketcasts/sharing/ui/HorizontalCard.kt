package au.com.shiftyjelly.pocketcasts.sharing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
internal fun HorizontalPodcastCast(
    podcast: Podcast,
    episodeCount: Int,
    shareColors: ShareColors,
    modifier: Modifier = Modifier,
) = HorizontalCard(
    type = PodcastCardType(
        podcast = podcast,
        episodeCount = episodeCount,
    ),
    shareColors = shareColors,
    modifier = modifier,
)

@Composable
internal fun HorizontalEpisodeCard(
    episode: PodcastEpisode,
    podcast: Podcast,
    useEpisodeArtwork: Boolean,
    shareColors: ShareColors,
    modifier: Modifier = Modifier,
) = HorizontalCard(
    type = EpisodeCardType(
        episode = episode,
        podcast = podcast,
        useEpisodeArtwork = useEpisodeArtwork,
    ),
    shareColors = shareColors,
    modifier = modifier,
)

@Composable
private fun HorizontalCard(
    type: CardType,
    shareColors: ShareColors,
    modifier: Modifier = Modifier,
) = BoxWithConstraints {
    val backgroundGradient = Brush.verticalGradient(
        listOf(
            shareColors.cardTop,
            shareColors.cardBottom,
        ),
    )
    val cardHeight = maxWidth * 0.52f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(backgroundGradient, RoundedCornerShape(12.dp))
            .width(maxWidth)
            .height(cardHeight),
    ) {
        Spacer(
            modifier = Modifier.width(cardHeight * 0.15f),
        )
        type.Image(
            modifier = Modifier
                .size(cardHeight * 0.7f)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(
            modifier = Modifier.width(cardHeight * 0.15f),
        )
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(end = cardHeight * 0.15f),
        ) {
            TextH70(
                text = type.topText(),
                color = shareColors.cardText.copy(alpha = 0.5f),
                maxLines = 1,
            )
            Spacer(
                modifier = Modifier.height(6.dp),
            )
            TextH40(
                text = type.middleText(),
                color = shareColors.cardText,
                maxLines = 3,
            )
            Spacer(
                modifier = Modifier.height(6.dp),
            )
            TextH70(
                text = type.bottomText(),
                maxLines = 2,
                color = shareColors.cardText.copy(alpha = 0.5f),
            )
        }
    }
}

@ShowkaseComposable(name = "HorizontalPodcastCard", group = "Sharing", styleName = "Light")
@Preview(name = "HorizontalPodcastCardLight")
@Composable
fun HorizontalPodcastCardLightPreview() = HorizontalPodcastCardPreview(
    baseColor = Color(0xFFFBCB04),
)

@ShowkaseComposable(name = "HorizontalPodcastCard", group = "Sharing", styleName = "Dark")
@Preview(name = "HorizontalPodcastCardDark")
@Composable
fun HorizontalPodcastCardDarkPreview() = HorizontalPodcastCardPreview(
    baseColor = Color(0xFFEC0404),
)

@ShowkaseComposable(name = "HorizontalEpisodeCard", group = "Sharing", styleName = "Light")
@Preview(name = "HorizontalEpisodeCardLight")
@Composable
fun HorizontalEpisodeCardLightPreview() = HorizontalEpisodeCardPreview(
    baseColor = Color(0xFFFBCB04),
)

@ShowkaseComposable(name = "HorizontalEpisodeCard", group = "Sharing", styleName = "Dark")
@Preview(name = "HorizontalEpisodeCardDark")
@Composable
fun HorizontalEpisodeCardDarkPreview() = HorizontalEpisodeCardPreview(
    baseColor = Color(0xFFEC0404),
)

@Composable
private fun HorizontalPodcastCardPreview(
    baseColor: Color,
) = HorizontalCardPreview(
    type = PodcastCardType(
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
    type = EpisodeCardType(
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
    type: CardType,
    baseColor: Color,
) = HorizontalCard(
    type = type,
    shareColors = ShareColors(baseColor),
)
