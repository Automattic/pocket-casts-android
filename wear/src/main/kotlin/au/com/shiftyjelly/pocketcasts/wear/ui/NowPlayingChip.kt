package au.com.shiftyjelly.pocketcasts.wear.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.ui.di.WearImageLoader
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.wear.ui.component.NowPlayingAnimation
import au.com.shiftyjelly.pocketcasts.wear.ui.component.WatchListChip
import coil3.ImageLoader
import coil3.compose.rememberAsyncImagePainter
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun NowPlayingChip(
    viewModel: NowPlayingChipViewModel = hiltViewModel(),
    onClick: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val playbackState = state.playbackState
    val artworkConfiguration by viewModel.artworkConfiguration.collectAsState()

    val upNextQueue = state.upNextQueue
    val podcast = (upNextQueue as? UpNextQueue.State.Loaded)?.podcast
    val episode = (upNextQueue as? UpNextQueue.State.Loaded)?.episode

    Content(
        podcast = podcast,
        episode = episode,
        isPlaying = playbackState?.isPlaying == true,
        useEpisodeArtwork = artworkConfiguration.useEpisodeArtwork,
        imageLoader = viewModel.imageLoader,
        onClick = onClick,
    )
}

@Composable
private fun Content(
    podcast: Podcast?,
    episode: BaseEpisode?,
    isPlaying: Boolean,
    useEpisodeArtwork: Boolean,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val density = LocalDensity.current

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
            backgroundImagePainter = if (episode != null) {
                val imageRequest = remember(episode.uuid, useEpisodeArtwork, density) {
                    // set the image size or the image crop won't work correctly
                    val imageWidthPx = with(density) {
                        resources.displayMetrics.widthPixels - (ChipDefaults.ChipHorizontalPadding.toPx() * 2).toInt()
                    }
                    val imageHeightPx = with(density) {
                        ChipDefaults.Height.toPx().toInt()
                    }
                    PocketCastsImageRequestFactory(context).themed()
                        .create(episode, useEpisodeArtwork)
                        .newBuilder()
                        .size(
                            width = imageWidthPx,
                            height = imageHeightPx,
                        )
                        .build()
                }

                rememberAsyncImagePainter(
                    model = imageRequest,
                    imageLoader = imageLoader,
                    contentScale = ContentScale.Crop,
                )
            } else {
                nothingPainter
            },
            // only want a scrim if there is a episode background
            backgroundImageScrimBrush = if (episode != null) {
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colors.surface,
                        MaterialTheme.colors.surface.copy(alpha = 0f),
                    ),
                )
            } else {
                SolidColor(MaterialTheme.colors.surface)
            },
            contentColor = MaterialTheme.colors.onPrimary,
            secondaryContentColor = MaterialTheme.colors.onSecondary,
        ),
        onClick = onClick,
        modifier = Modifier
            .height(ChipDefaults.Height)
            .fillMaxWidth(), // This is needed for the backgroundImagePainter to work
    )
}

private val nothingPainter = object : Painter() {
    override val intrinsicSize = Size.Unspecified
    override fun DrawScope.onDraw() { /* do nothing */ }
}

@Composable
private fun PlayingAnimation() {
    NowPlayingAnimation()
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
    val context = LocalContext.current
    WearAppTheme {
        Content(
            podcast = Podcast(
                uuid = "b643cb50-2c52-013b-ef7a-0acc26574db2",
                title = "A Podcast Title",
            ),
            episode = PodcastEpisode(
                title = "An Episode",
                uuid = "57853d71-30ac-4477-af73-e8fe2b1d4dda",
                publishedDate = Date(),
            ),
            useEpisodeArtwork = false,
            isPlaying = false,
            imageLoader = ImageLoader(context),
            onClick = {},
        )
    }
}
