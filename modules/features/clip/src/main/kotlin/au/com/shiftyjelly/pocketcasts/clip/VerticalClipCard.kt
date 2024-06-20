package au.com.shiftyjelly.pocketcasts.clip

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.EpisodeImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory.PlaceholderType
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatMediumStyle
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import java.sql.Date
import java.time.Instant
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
internal fun VerticalClipCard(
    episode: PodcastEpisode,
    podcast: Podcast,
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.background(backgroundGradient, RoundedCornerShape(12.dp)),
    ) {
        Spacer(
            modifier = Modifier.height(46.dp),
        )
        EpisodeImage(
            episode = episode,
            useEpisodeArtwork = useEpisodeArtwork,
            placeholderType = if (LocalInspectionMode.current) PlaceholderType.Large else PlaceholderType.None,
            modifier = Modifier
                .padding(horizontal = 46.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp)),
        )

        Spacer(
            modifier = Modifier.height(24.dp),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            TextH70(
                text = episode.publishedDate.toLocalizedFormatMediumStyle(),
                color = clipColors.cardTextColor.copy(alpha = 0.5f),
            )
            Spacer(
                modifier = Modifier.height(6.dp),
            )
            TextH40(
                text = episode.title,
                color = clipColors.cardTextColor,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
            Spacer(
                modifier = Modifier.height(6.dp),
            )
            TextH70(
                text = podcast.title,
                color = clipColors.cardTextColor.copy(alpha = 0.5f),
                maxLines = 1,
            )
        }

        Spacer(
            modifier = Modifier.height(26.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color(0xFFF43E37), RoundedCornerShape(24.dp))
                .height(24.dp)
                .padding(start = 4.dp, end = 8.dp),
        ) {
            Image(
                painter = painterResource(id = IR.drawable.ic_logo_foreground),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(
                modifier = Modifier.width(8.dp),
            )
            TextH70(
                text = "Pocket Casts",
                color = Color.White,
            )
        }
        Spacer(
            modifier = Modifier.height(28.dp),
        )
    }
}

@ShowkaseComposable(name = "VerticalClipCard", group = "Clip", styleName = "Light")
@Preview(name = "VerticalClipCardLight")
@Composable
fun VerticalClipCardLightPreview() = VerticalClipCardPreview(Color(0xFF9BF6FF))

@ShowkaseComposable(name = "VerticalClipCard", group = "Clip", styleName = "Dark")
@Preview(name = "VerticalClipCardDark")
@Composable
fun VerticalClipCardDarkPreview() = VerticalClipCardPreview(Color(0xFF152622))

@Composable
private fun VerticalClipCardPreview(
    baseColor: Color,
) = VerticalClipCard(
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
    clipColors = ClipColors(baseColor),
)
