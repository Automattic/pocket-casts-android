package au.com.shiftyjelly.pocketcasts.clip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.EpisodeImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory.PlaceholderType
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import java.sql.Date
import java.time.Instant
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun HorizontalClipCard(
    episode: PodcastEpisode,
    podcast: Podcast,
    episodeCount: Int,
    useEpisodeArtwork: Boolean,
    clipColors: ClipColors,
    modifier: Modifier = Modifier,
) {
    val backgroundGradient = Brush.verticalGradient(
        listOf(
            clipColors.cardTop,
            clipColors.cardBottom,
        ),
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(backgroundGradient, RoundedCornerShape(12.dp))
            .padding(24.dp),
    ) {
        EpisodeImage(
            episode = episode,
            useEpisodeArtwork = useEpisodeArtwork,
            placeholderType = if (LocalInspectionMode.current) PlaceholderType.Large else PlaceholderType.None,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)),
        )
        Spacer(
            modifier = Modifier.width(18.dp),
        )
        Column(
            verticalArrangement = Arrangement.Center,
        ) {
            TextH70(
                text = podcast.title,
                color = clipColors.cardTextColor.copy(alpha = 0.5f),
                maxLines = 1,
            )
            Spacer(
                modifier = Modifier.height(6.dp),
            )
            TextH40(
                text = episode.title,
                color = clipColors.cardTextColor,
                maxLines = 1,
            )
            Spacer(
                modifier = Modifier.height(6.dp),
            )
            TextH70(
                text = listOfNotNull(
                    pluralStringResource(LR.plurals.episode_count, count = episodeCount, episodeCount),
                    podcast.displayableFrequency(LocalContext.current.resources),
                ).joinToString(" · "),
                color = clipColors.cardTextColor.copy(alpha = 0.5f),
            )
        }
    }
}

@ShowkaseComposable(name = "HorizontalClipCard", group = "Clip", styleName = "Light")
@Preview(name = "HorizontalClipCardLight")
@Composable
fun HorizontalClipCardLightPreview() = HorizontalClipCardPreview(Color(0xFF9BF6FF))

@ShowkaseComposable(name = "HorizontalClipCard", group = "Clip", styleName = "Dark")
@Preview(name = "HorizontalClipCardDark")
@Composable
fun HorizontalClipCardDarkPreview() = HorizontalClipCardPreview(Color(0xFF152622))

@Composable
private fun HorizontalClipCardPreview(
    baseColor: Color,
) = HorizontalClipCard(
    episode = PodcastEpisode(
        uuid = "episode-id",
        podcastUuid = "podcast-id",
        publishedDate = Date.from(Instant.parse("2024-12-03T10:15:30.00Z")),
        title = "Nobis sapiente fugit vitae. Iusto magnam nam nam et odio. Debitis cupiditate officiis et. Sit quia in voluptate sit voluptatem magni.",
    ),
    podcast = Podcast(
        uuid = "podcast-id",
        title = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
        episodeFrequency = "monthly",
    ),
    episodeCount = 120,
    useEpisodeArtwork = true,
    clipColors = ClipColors(baseColor),
    modifier = Modifier.height(172.dp),
)
