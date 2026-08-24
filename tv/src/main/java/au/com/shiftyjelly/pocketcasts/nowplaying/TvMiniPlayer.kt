package au.com.shiftyjelly.pocketcasts.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.TvArtworkImage
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import java.util.Date

@Composable
fun TvMiniPlayer(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TvNowPlayingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loaded = uiState as? TvNowPlayingUiState.Loaded

    val currentOnDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(visible, loaded == null) {
        if (visible && loaded == null) {
            currentOnDismiss()
        }
    }

    var retained by remember { mutableStateOf<TvNowPlayingUiState.Loaded?>(null) }
    if (loaded != null) {
        retained = loaded
    }

    AnimatedVisibility(
        visible = visible && loaded != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier.fillMaxWidth(),
    ) {
        retained?.let { state ->
            MiniPlayerContent(
                state = state,
                onPlayPause = viewModel::playPause,
                onSkipBackward = viewModel::skipBackward,
                onSkipForward = viewModel::skipForward,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun MiniPlayerContent(
    state: TvNowPlayingUiState.Loaded,
    onPlayPause: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val seekBarFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos {}
        runCatching { seekBarFocusRequester.requestFocus() }
    }
    BackHandler(onBack = onDismiss)

    val episode = state.episode
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(DrawerShape)
            .background(MaterialTheme.tvColors.overlayContainer)
            .border(1.dp, MaterialTheme.tvColors.overlayBorder, DrawerShape)
            .padding(horizontal = DrawerHorizontalInset, vertical = 24.dp),
    ) {
        TvArtworkImage(
            model = episode.artworkModel(),
            modifier = Modifier
                .size(ArtworkSize)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(modifier = Modifier.width(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            state.podcastTitle?.let { podcastTitle ->
                Text(
                    text = podcastTitle,
                    style = MaterialTheme.tvTypography.caption1,
                    color = MaterialTheme.tvColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = episode.title,
                style = MaterialTheme.tvTypography.callout,
                color = MaterialTheme.tvColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(16.dp))
            TvSeekBar(
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                onSkipBack = onSkipBackward,
                onSkipForward = onSkipForward,
                onPlayPause = onPlayPause,
                focusRequester = seekBarFocusRequester,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val DrawerShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
private val DrawerHorizontalInset = 56.dp
private val ArtworkSize = 96.dp

@Preview(widthDp = 960)
@Composable
private fun TvMiniPlayerPreview() {
    TvTheme {
        MiniPlayerContent(
            state = TvNowPlayingUiState.Loaded(
                episode = PodcastEpisode(
                    uuid = "episode-uuid",
                    title = "Episode title that might be quite long and wrap onto two lines",
                    podcastUuid = "podcast-uuid",
                    publishedDate = Date(0),
                ),
                podcastTitle = "Podcast title",
                isPlaying = true,
                isBuffering = false,
                errorMessage = null,
                positionMs = 600_000,
                durationMs = 3_600_000,
                bufferedMs = 1_200_000,
                isVideo = false,
                player = null,
                playbackSpeed = 1.0,
                trimMode = TrimMode.OFF,
                isVolumeBoosted = false,
            ),
            onPlayPause = {},
            onSkipBackward = {},
            onSkipForward = {},
            onDismiss = {},
        )
    }
}
