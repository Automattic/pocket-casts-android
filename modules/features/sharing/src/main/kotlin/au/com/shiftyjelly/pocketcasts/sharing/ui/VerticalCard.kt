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
) = VerticalCard(
    type = PodcastCardType(
        podcast = podcast,
        episodeCount = episodeCount,
    ),
    shareColors = shareColors,
    modifier = modifier,
)

@Composable
internal fun VerticalEpisodeCard(
    episode: PodcastEpisode,
    podcast: Podcast,
    useEpisodeArtwork: Boolean,
    shareColors: ShareColors,
    modifier: Modifier = Modifier,
) = VerticalCard(
    type = EpisodeCardType(
        episode = episode,
        podcast = podcast,
        useEpisodeArtwork = useEpisodeArtwork,
    ),
    shareColors = shareColors,
    modifier = modifier,
)

@Composable
private fun VerticalCard(
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(backgroundGradient, RoundedCornerShape(12.dp))
            .width(maxWidth)
            .height(maxWidth * 1.5f),
    ) {
        Spacer(
            modifier = Modifier.height(this@BoxWithConstraints.maxWidth * 0.15f),
        )
        type.Image(
            modifier = Modifier
                .size(this@BoxWithConstraints.maxWidth * 0.7f)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(
            modifier = Modifier.height(24.dp),
        )
        TextH70(
            text = type.topText(),
            maxLines = 1,
            color = shareColors.cardText.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 64.dp),
        )
        Spacer(
            modifier = Modifier.height(6.dp),
        )
        TextH40(
            text = type.middleText(),
            maxLines = 2,
            textAlign = TextAlign.Center,
            color = shareColors.cardText,
            modifier = Modifier.padding(horizontal = 42.dp),
        )
        Spacer(
            modifier = Modifier.height(6.dp),
        )
        TextH70(
            text = type.bottomText(),
            maxLines = 2,
            textAlign = TextAlign.Center,
            color = shareColors.cardText.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 64.dp),
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
private fun VerticalEpisodeCardPreview(
    baseColor: Color,
) = VerticalCardPreview(
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
private fun VerticalCardPreview(
    type: CardType,
    baseColor: Color,
) = VerticalCard(
    type = type,
    shareColors = ShareColors(baseColor),
)
