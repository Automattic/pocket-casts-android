package au.com.shiftyjelly.pocketcasts.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.TvArtworkImage
import au.com.shiftyjelly.pocketcasts.component.TvEmptyState
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.TvTopBarHeight
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvNowPlayingScreen(
    modifier: Modifier = Modifier,
    viewModel: TvNowPlayingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is TvNowPlayingUiState.Empty -> TvEmptyState(
            title = stringResource(LR.string.tv_nothing_playing_title),
            subtitle = stringResource(LR.string.tv_nothing_playing_subtitle),
            modifier = modifier
                .fillMaxSize()
                .padding(top = TvTopBarHeight),
        )

        is TvNowPlayingUiState.Loaded -> TvNowPlayingContent(
            state = state,
            onPlayPause = viewModel::playPause,
            onSkipBackward = viewModel::skipBackward,
            onSkipForward = viewModel::skipForward,
            modifier = modifier,
        )
    }
}

@Composable
private fun TvNowPlayingContent(
    state: TvNowPlayingUiState.Loaded,
    onPlayPause: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = TvTopBarHeight)
            .padding(horizontal = 80.dp, vertical = 24.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            EpisodeArtworkWithTitles(state = state)
        }
        Spacer(modifier = Modifier.height(24.dp))
        state.errorMessage?.takeIf { state.isError }?.let { errorMessage ->
            Text(
                text = errorMessage,
                style = MaterialTheme.tvTypography.caption1,
                color = MaterialTheme.tvColors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            )
        }
        TvSeekBar(
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            bufferedMs = state.bufferedMs,
            onSeekBack = onSkipBackward,
            onSeekForward = onSkipForward,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        PlayerControls(
            isPlaying = state.isPlaying,
            isBuffering = state.isBuffering,
            onPlayPause = onPlayPause,
            onSkipBackward = onSkipBackward,
            onSkipForward = onSkipForward,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun EpisodeArtworkWithTitles(
    state: TvNowPlayingUiState.Loaded,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        TvArtworkImage(
            model = state.episode.artworkModel(),
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = state.episode.title,
            style = MaterialTheme.tvTypography.title3,
            color = MaterialTheme.tvColors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        state.podcastTitle?.let { podcastTitle ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = podcastTitle,
                style = MaterialTheme.tvTypography.caption1,
                color = MaterialTheme.tvColors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PlayerControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlayPause: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        PlayerControlButton(
            iconRes = IR.drawable.wear_skip_back,
            contentDescription = stringResource(LR.string.skip_back),
            onClick = onSkipBackward,
        )
        PlayPauseButton(
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            onClick = onPlayPause,
        )
        PlayerControlButton(
            iconRes = IR.drawable.wear_skip_foreward,
            contentDescription = stringResource(LR.string.skip_forward),
            onClick = onSkipForward,
        )
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        colors = TvButtonDefaults.iconButtonColors(),
        modifier = modifier.size(64.dp),
    ) {
        if (isBuffering) {
            LoadingView(color = LocalContentColor.current)
        } else {
            Icon(
                painter = painterResource(if (isPlaying) IR.drawable.button_pause else IR.drawable.button_play),
                contentDescription = stringResource(if (isPlaying) LR.string.pause else LR.string.play),
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

@Composable
private fun PlayerControlButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 56.dp,
    iconSize: Dp = 24.dp,
) {
    IconButton(
        onClick = onClick,
        colors = TvButtonDefaults.iconButtonColors(),
        modifier = modifier.size(buttonSize),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
        )
    }
}

private fun BaseEpisode.artworkModel(): Any? = when (this) {
    is PodcastEpisode -> PodcastImage.getArtworkUrl(size = null, uuid = podcastUuid, isWearOS = false)
    is UserEpisode -> artworkUrl
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvNowPlayingContentPreview() {
    TvTheme {
        TvNowPlayingContent(
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
                isError = false,
                errorMessage = null,
                positionMs = 600_000,
                durationMs = 3_600_000,
                bufferedMs = 1_200_000,
                isVideo = false,
                player = null,
            ),
            onPlayPause = {},
            onSkipBackward = {},
            onSkipForward = {},
        )
    }
}
