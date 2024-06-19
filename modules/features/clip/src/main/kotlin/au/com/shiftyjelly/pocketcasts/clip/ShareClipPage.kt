package au.com.shiftyjelly.pocketcasts.clip

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import java.sql.Date
import java.time.Instant
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ShareClipPage(
    episode: PodcastEpisode?,
    podcastTitle: String,
    useEpisodeArtwork: Boolean,
    baseColor: Color,
    onClose: () -> Unit,
) {
    val backgroundColor = ColorUtils.changeHsvValue(baseColor, factor = 0.4f)
    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor),
        ) {
            Spacer(
                modifier = Modifier.weight(0.3f),
            )

            TextH30(
                text = stringResource(LR.string.podcast_create_clip),
                color = if (backgroundColor.luminance() < 0.5f) Color.White else Color.Black,
            )

            Spacer(
                modifier = Modifier.weight(1f),
            )

            if (episode != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 50.dp),
                ) {
                    ClipCard(
                        episode = episode,
                        podcastTitle = podcastTitle,
                        useEpisodeArtwork = useEpisodeArtwork,
                        baseColor = baseColor,
                    )
                }
                Spacer(
                    modifier = Modifier.weight(1f),
                )
            }

            ClipSelector(
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(
                modifier = Modifier.height(16.dp),
            )
            RowButton(
                text = stringResource(LR.string.podcast_share_clip),
                onClick = { },
                colors = ButtonDefaults.buttonColors(backgroundColor = baseColor),
                textColor = if (baseColor.luminance() < 0.5f) Color.White else Color.Black,
                elevation = null,
                includePadding = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        ) {
            Image(
                painter = painterResource(IR.drawable.ic_close_sheet),
                contentDescription = stringResource(LR.string.close),
                modifier = Modifier
                    .padding(top = 16.dp, end = 16.dp)
                    .clickable(onClick = onClose),
            )
        }
    }
}

@ShowkaseComposable(name = "ShareClipPage", group = "Clip")
@Preview(name = "ShareClipPage")
@Composable
fun ShareClipPagePreview() = ShareClipPage(
    episode = PodcastEpisode(
        uuid = "episode-id",
        podcastUuid = "podcast-id",
        publishedDate = Date.from(Instant.parse("2024-12-03T10:15:30.00Z")),
        title = "Episode title",
    ),
    podcastTitle = "Podcast title",
    useEpisodeArtwork = true,
    baseColor = Color(0xFF9BF6FF),
    onClose = {},
)
