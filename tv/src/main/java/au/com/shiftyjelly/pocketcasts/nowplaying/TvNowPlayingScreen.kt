package au.com.shiftyjelly.pocketcasts.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import au.com.shiftyjelly.pocketcasts.component.HideTvTopBar
import au.com.shiftyjelly.pocketcasts.component.TvArtworkImage
import au.com.shiftyjelly.pocketcasts.component.TvEmptyState
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeActionContext
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeActionsModal
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeInfoModal
import au.com.shiftyjelly.pocketcasts.component.TvMoreButton
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.podcasts.TvPodcastDetailsScreen
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.TvTopBarHeight
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import java.util.Date
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvNowPlayingScreen(
    openTrigger: Int,
    modifier: Modifier = Modifier,
    viewModel: TvNowPlayingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var openedPodcastUuid by rememberSaveable { mutableStateOf<String?>(null) }

    // Skip the first composition so a restored openedPodcastUuid is not cleared.
    var lastHandledOpenTrigger by remember { mutableIntStateOf(openTrigger) }
    LaunchedEffect(openTrigger) {
        if (openTrigger != lastHandledOpenTrigger) {
            lastHandledOpenTrigger = openTrigger
            openedPodcastUuid = null
        }
    }

    val podcastUuid = openedPodcastUuid
    if (podcastUuid != null) {
        HideTvTopBar()
        BackHandler { openedPodcastUuid = null }
        TvPodcastDetailsScreen(
            podcastUuid = podcastUuid,
            onClose = { openedPodcastUuid = null },
            modifier = modifier,
        )
        return
    }

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
            onOpenPodcast = { openedPodcastUuid = it },
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
    onOpenPodcast: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isActionsModalVisible by remember { mutableStateOf(false) }
    var isDetailsModalVisible by remember { mutableStateOf(false) }
    var isChromeVisible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }
    val episode = state.episode

    LaunchedEffect(state.isPlaying, isActionsModalVisible, isDetailsModalVisible, interactionTick) {
        if (state.isPlaying && !isActionsModalVisible && !isDetailsModalVisible) {
            delay(CHROME_HIDE_DELAY)
            isChromeVisible = false
        } else {
            isChromeVisible = true
        }
    }
    if (!isChromeVisible) {
        HideTvTopBar()
    }
    val chromeAlpha by animateFloatAsState(if (isChromeVisible) 1f else 0f, label = "TvNowPlayingChromeAlpha")

    Column(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                interactionTick++
                val revealsChrome = !isChromeVisible
                if (revealsChrome) {
                    isChromeVisible = true
                }
                // Only navigation presses are swallowed by the reveal; media and back keys keep
                // their effect even while the chrome is hidden.
                revealsChrome && event.key in chromeRevealConsumedKeys
            }
            .padding(top = TvTopBarHeight)
            .padding(horizontal = 80.dp, vertical = 24.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (state.isVideo) {
                TvVideoSurface(player = state.player)
            } else {
                EpisodeArtworkWithTitles(episode = episode, podcastTitle = state.podcastTitle)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Column(modifier = Modifier.alpha(chromeAlpha)) {
            state.errorMessage?.let { errorMessage ->
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
                onSkipBack = onSkipBackward,
                onSkipForward = onSkipForward,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            PlayerControls(
                isPlaying = state.isPlaying,
                isBuffering = state.isBuffering,
                onPlayPause = onPlayPause,
                onSkipBackward = onSkipBackward,
                onSkipForward = onSkipForward,
                onOpenActions = if (episode is PodcastEpisode) {
                    { isActionsModalVisible = true }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (episode is PodcastEpisode) {
        if (isActionsModalVisible) {
            TvEpisodeActionsModal(
                episode = episode,
                actionContext = TvEpisodeActionContext.NowPlaying,
                onDismissRequest = { isActionsModalVisible = false },
                onShowEpisodeDetails = {
                    isDetailsModalVisible = true
                    isActionsModalVisible = false
                },
                onGoToPodcast = { onOpenPodcast(episode.podcastUuid) },
            )
        }
        if (isDetailsModalVisible) {
            TvEpisodeInfoModal(
                episode = episode,
                onDismissRequest = { isDetailsModalVisible = false },
            )
        }
    }
}

@Composable
private fun EpisodeArtworkWithTitles(
    episode: BaseEpisode,
    podcastTitle: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        TvArtworkImage(
            model = episode.artworkModel(),
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = episode.title,
            style = MaterialTheme.tvTypography.title3,
            color = MaterialTheme.tvColors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        podcastTitle?.let { podcastTitle ->
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
    onOpenActions: (() -> Unit)?,
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
        if (onOpenActions != null) {
            TvMoreButton(onClick = onOpenActions)
        }
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

private val CHROME_HIDE_DELAY = 4.seconds

private val chromeRevealConsumedKeys = setOf(
    Key.DirectionUp,
    Key.DirectionDown,
    Key.DirectionLeft,
    Key.DirectionRight,
    Key.DirectionCenter,
    Key.Enter,
)

private fun BaseEpisode.artworkModel(): Any? = when (this) {
    is PodcastEpisode -> PodcastImage.getMediumArtworkUrl(podcastUuid)
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
            onOpenPodcast = {},
        )
    }
}
