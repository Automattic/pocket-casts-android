package au.com.shiftyjelly.pocketcasts.wear.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.ui.images.PodcastImageLoaderThemed
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.wear.ui.component.WatchListChip
import coil.compose.rememberAsyncImagePainter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun NowPlayingChip(
    onClick: () -> Unit,
) {

    val viewModel = hiltViewModel<NowPlayingChipViewModel>()

    val state by viewModel.state.collectAsState()
    val playbackState = state.playbackState

    val upNextQueue = state.upNextQueue
    val podcast = (upNextQueue as? UpNextQueue.State.Loaded)?.podcast
    val episode = (upNextQueue as? UpNextQueue.State.Loaded)?.episode

    Content(
        podcast = podcast,
        episode = episode,
        isPlaying = playbackState?.isPlaying == true,
        onClick = onClick
    )
}

@Composable
private fun Content(
    podcast: Podcast?,
    episode: BaseEpisode?,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current

    WatchListChip(
        title = if (isPlaying) {
            stringResource(LR.string.player_tab_playing_wide)
        } else {
            podcast?.title ?: stringResource(LR.string.player_tab_playing_wide)
        },
        icon = {
            if (isPlaying) {
                PlayingAnimation()
            } else {
                PlayIcon()
            }
        },
        secondaryLabel = episode?.title,
        colors = ChipDefaults.imageBackgroundChipColors(
            backgroundImagePainter = if (podcast != null) {
                val imageRequest = remember(podcast.uuid) {
                    PodcastImageLoaderThemed(context).loadCoil(podcastUuid = podcast.uuid).build()
                }
                rememberAsyncImagePainter(
                    model = imageRequest,
                    contentScale = ContentScale.Crop
                )
            } else {
                nothingPainter
            },
            backgroundImageScrimBrush =
            // only want a scrim if there is a podcast background
            if (podcast != null) {
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colors.surface,
                        MaterialTheme.colors.surface.copy(alpha = 0f)
                    )
                )
            } else {
                SolidColor(MaterialTheme.colors.surface)
            },
            contentColor = MaterialTheme.colors.onPrimary,
            secondaryContentColor = MaterialTheme.colors.onSecondary,
        ),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth() // This is needed for the backgroundImagePainter to work
    )
}

private val nothingPainter = object : Painter() {
    override val intrinsicSize = Size.Unspecified
    override fun DrawScope.onDraw() { /* do nothing */ }
}

@Composable
private fun PlayingAnimation() {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(IR.raw.nowplaying)
    )
    LottieAnimation(
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )
}

@Composable
private fun PlayIcon() {
    Icon(
        painter = painterResource(IR.drawable.ic_play_all),
        contentDescription = null,
    )
}

@Preview(
    widthDp = 180,
    heightDp = 60,
    uiMode = Configuration.UI_MODE_TYPE_WATCH,
)
@Composable
private fun Preview() {
    WearAppTheme {
        Content(
            podcast = Podcast(
                uuid = "b643cb50-2c52-013b-ef7a-0acc26574db2",
                title = "A Podcast Title",
            ),
            episode = PodcastEpisode(
                title = "An Episode",
                uuid = "57853d71-30ac-4477-af73-e8fe2b1d4dda",
                publishedDate = Date()
            ),
            isPlaying = false,
            onClick = {},
        )
    }
}
