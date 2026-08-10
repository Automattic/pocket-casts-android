package au.com.shiftyjelly.pocketcasts.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import au.com.shiftyjelly.pocketcasts.component.LocalFocusTvTopBar
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
import au.com.shiftyjelly.pocketcasts.repositories.playback.Player
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.TvScreenBackgroundBrush
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
    isOpenRequested: Boolean,
    onConsumeOpenRequest: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TvNowPlayingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var openedPodcastUuid by rememberSaveable { mutableStateOf<String?>(null) }
    val currentOnConsumeOpenRequest by rememberUpdatedState(onConsumeOpenRequest)

    val podcastUuid = openedPodcastUuid.takeUnless { isOpenRequested }
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
        is TvNowPlayingUiState.Empty -> {
            LaunchedEffect(isOpenRequested) {
                if (isOpenRequested) {
                    currentOnConsumeOpenRequest()
                }
            }
            TvEmptyState(
                title = stringResource(LR.string.tv_nothing_playing_title),
                subtitle = stringResource(LR.string.tv_nothing_playing_subtitle),
                modifier = modifier
                    .fillMaxSize()
                    .padding(top = TvTopBarHeight),
            )
        }

        is TvNowPlayingUiState.Loaded -> TvNowPlayingContent(
            state = state,
            isPlayerFocusRequested = isOpenRequested,
            onConsumePlayerFocusRequest = {
                openedPodcastUuid = null
                onConsumeOpenRequest()
            },
            onPlayPause = viewModel::playPause,
            onSkipBackward = viewModel::skipBackward,
            onSkipForward = viewModel::skipForward,
            onSelectSpeed = viewModel::setPlaybackSpeed,
            onSetVolumeBoost = viewModel::setVolumeBoost,
            onSelectTrimMode = viewModel::setTrimMode,
            onOpenPodcast = { openedPodcastUuid = it },
            modifier = modifier,
        )
    }
}

@Composable
private fun TvNowPlayingContent(
    state: TvNowPlayingUiState.Loaded,
    isPlayerFocusRequested: Boolean,
    onConsumePlayerFocusRequest: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    onSelectSpeed: (Double) -> Unit,
    onSetVolumeBoost: (Boolean) -> Unit,
    onSelectTrimMode: (TrimMode) -> Unit,
    onOpenPodcast: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isActionsModalVisible by remember { mutableStateOf(false) }
    var isDetailsModalVisible by remember { mutableStateOf(false) }
    var isSpeedMenuVisible by remember { mutableStateOf(false) }
    var isEffectsMenuVisible by remember { mutableStateOf(false) }
    var isChromeVisible by remember { mutableStateOf(true) }
    var hasRenderedFirstFrame by remember(state.episode.uuid, state.isVideo) { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var isContentFocused by remember { mutableStateOf(false) }
    var isTopBarRevealRequested by remember { mutableStateOf(false) }
    val focusTopBar = LocalFocusTvTopBar.current
    val playPauseFocusRequester = remember { FocusRequester() }
    val episode = state.episode

    val currentOnConsumePlayerFocusRequest by rememberUpdatedState(onConsumePlayerFocusRequest)
    LaunchedEffect(isPlayerFocusRequested) {
        if (isPlayerFocusRequested) {
            withFrameNanos { }
            runCatching { playPauseFocusRequester.requestFocus() }
            currentOnConsumePlayerFocusRequest()
        }
    }
    val isAnyOverlayVisible =
        isActionsModalVisible || isDetailsModalVisible || isSpeedMenuVisible || isEffectsMenuVisible

    LaunchedEffect(state.isPlaying, isAnyOverlayVisible, interactionTick) {
        if (state.isPlaying && !isAnyOverlayVisible) {
            delay(CHROME_HIDE_DELAY)
            isChromeVisible = false
            if (!isContentFocused) {
                runCatching { playPauseFocusRequester.requestFocus() }
            }
        } else {
            isChromeVisible = true
        }
    }
    if ((isContentFocused || isAnyOverlayVisible) && !isTopBarRevealRequested) {
        HideTvTopBar()
    }
    if (!isChromeVisible) {
        BackHandler {
            interactionTick++
            isChromeVisible = true
        }
    } else if (isContentFocused) {
        BackHandler {
            interactionTick++
            isTopBarRevealRequested = true
            focusTopBar()
        }
    }
    val chromeAlpha by animateFloatAsState(if (isChromeVisible) 1f else 0f, label = "TvNowPlayingChromeAlpha")

    Box(
        modifier = modifier
            .fillMaxSize()
            .onFocusChanged { focusState ->
                isContentFocused = focusState.hasFocus
                if (!focusState.hasFocus) {
                    isTopBarRevealRequested = false
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || event.key == Key.Back) return@onPreviewKeyEvent false
                interactionTick++
                val revealsChrome = !isChromeVisible
                if (revealsChrome) {
                    isChromeVisible = true
                }
                // Only navigation presses are swallowed by the reveal; media keys keep their
                // effect even while the chrome is hidden and back is revealed via BackHandler.
                revealsChrome && event.key in chromeRevealConsumedKeys
            },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .then(if (state.isVideo) Modifier.background(Color.Black) else Modifier),
        ) {
            if (state.isVideo) {
                TvVideoSurface(
                    player = state.player,
                    onFirstFrameRender = { hasRenderedFirstFrame = true },
                    onVideoReset = { hasRenderedFirstFrame = false },
                )
            } else {
                EpisodeArtwork(
                    episode = episode,
                    isPlaying = state.isPlaying && !state.isBuffering,
                    player = state.player,
                    audioLevel = { state.player?.currentAudioLevel ?: 0f },
                )
            }
        }
        if (state.isVideo) {
            AnimatedVisibility(
                visible = !hasRenderedFirstFrame || state.errorMessage != null,
                enter = fadeIn(tween(VIDEO_OVERLAY_FADE_MILLIS)),
                exit = fadeOut(tween(VIDEO_OVERLAY_FADE_MILLIS)),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(TvScreenBackgroundBrush),
                ) {
                    EpisodeArtwork(
                        episode = episode,
                        isPlaying = state.isPlaying && !state.isBuffering,
                        player = state.player,
                        audioLevel = { state.player?.currentAudioLevel ?: 0f },
                        showWaveform = false,
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer { alpha = chromeAlpha }
                .background(ChromeScrimBrush)
                .padding(horizontal = 80.dp)
                .padding(top = ChromeScrimTopInset, bottom = 24.dp),
        ) {
            EpisodeTitles(
                episode = episode,
                podcastTitle = state.podcastTitle,
            )
            Spacer(modifier = Modifier.height(16.dp))
            state.errorMessage?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    style = MaterialTheme.tvTypography.caption1,
                    color = MaterialTheme.tvColors.textSecondary,
                    textAlign = TextAlign.Start,
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
                playPauseFocusRequester = playPauseFocusRequester,
                playbackSpeed = state.playbackSpeed,
                trimMode = state.trimMode,
                isVolumeBoosted = state.isVolumeBoosted,
                isSpeedMenuVisible = isSpeedMenuVisible,
                isEffectsMenuVisible = isEffectsMenuVisible,
                onPlayPause = onPlayPause,
                onSkipBackward = onSkipBackward,
                onSkipForward = onSkipForward,
                onSpeedMenuVisibleChange = { isSpeedMenuVisible = it },
                onEffectsMenuVisibleChange = { isEffectsMenuVisible = it },
                onSelectSpeed = onSelectSpeed,
                onSetVolumeBoost = onSetVolumeBoost,
                onSelectTrimMode = onSelectTrimMode,
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
private fun EpisodeArtwork(
    episode: BaseEpisode,
    isPlaying: Boolean,
    player: Player?,
    audioLevel: () -> Float,
    modifier: Modifier = Modifier,
    showWaveform: Boolean = true,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        if (showWaveform) {
            TvNowPlayingWaveform(
                isPlaying = isPlaying,
                episodeUuid = episode.uuid,
                player = player,
                audioLevel = audioLevel,
                artworkSize = ArtworkSize,
            )
        }
        TvArtworkImage(
            model = episode.artworkModel(),
            modifier = Modifier
                .requiredSize(ArtworkSize * BlurredArtworkScale)
                .offset(x = BlurredArtworkOffset, y = BlurredArtworkOffset)
                .blur(BlurredArtworkRadius, BlurredEdgeTreatment.Unbounded)
                .alpha(0.7f),
        )
        TvArtworkImage(
            model = episode.artworkModel(),
            modifier = Modifier
                .size(ArtworkSize)
                .clip(RoundedCornerShape(8.dp)),
        )
    }
}

@Composable
private fun EpisodeTitles(
    episode: BaseEpisode,
    podcastTitle: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier,
    ) {
        Text(
            text = episode.title,
            style = MaterialTheme.tvTypography.title3,
            color = MaterialTheme.tvColors.textPrimary,
            textAlign = TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        podcastTitle?.let { podcastTitle ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = podcastTitle,
                style = MaterialTheme.tvTypography.caption1,
                color = MaterialTheme.tvColors.textSecondary,
                textAlign = TextAlign.Start,
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
    playPauseFocusRequester: FocusRequester,
    playbackSpeed: Double,
    trimMode: TrimMode,
    isVolumeBoosted: Boolean,
    isSpeedMenuVisible: Boolean,
    isEffectsMenuVisible: Boolean,
    onPlayPause: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    onSpeedMenuVisibleChange: (Boolean) -> Unit,
    onEffectsMenuVisibleChange: (Boolean) -> Unit,
    onSelectSpeed: (Double) -> Unit,
    onSetVolumeBoost: (Boolean) -> Unit,
    onSelectTrimMode: (TrimMode) -> Unit,
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
            modifier = Modifier.focusRequester(playPauseFocusRequester),
        )
        PlayerControlButton(
            iconRes = IR.drawable.wear_skip_foreward,
            contentDescription = stringResource(LR.string.skip_forward),
            onClick = onSkipForward,
        )
        TvPlaybackSpeedButton(
            speed = playbackSpeed,
            isMenuVisible = isSpeedMenuVisible,
            onMenuVisibleChange = onSpeedMenuVisibleChange,
            onSelectSpeed = onSelectSpeed,
        )
        TvPlayerEffectsButton(
            trimMode = trimMode,
            isVolumeBoosted = isVolumeBoosted,
            isMenuVisible = isEffectsMenuVisible,
            onMenuVisibleChange = onEffectsMenuVisibleChange,
            onSetVolumeBoost = onSetVolumeBoost,
            onSelectTrimMode = onSelectTrimMode,
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

private val ArtworkSize = 240.dp
private val BlurredArtworkScale = 1.25f
private val BlurredArtworkOffset = -ArtworkSize * 0.2f
private val BlurredArtworkRadius = 66.dp

private val CHROME_HIDE_DELAY = 4.seconds
private const val VIDEO_OVERLAY_FADE_MILLIS = 200

private val ChromeScrimTopInset = 48.dp
private val ChromeScrimBrush = Brush.verticalGradient(
    0f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.8f),
)

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
    TvNowPlayingContentPreviewContent(isVideo = false)
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvNowPlayingVideoContentPreview() {
    TvNowPlayingContentPreviewContent(isVideo = true)
}

@Composable
private fun TvNowPlayingContentPreviewContent(isVideo: Boolean) {
    TvTheme {
        CompositionLocalProvider(LocalFocusTvTopBar provides {}) {
            TvNowPlayingContent(
                isPlayerFocusRequested = false,
                onConsumePlayerFocusRequest = {},
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
                    isVideo = isVideo,
                    player = null,
                    playbackSpeed = 1.0,
                    trimMode = TrimMode.OFF,
                    isVolumeBoosted = false,
                ),
                onPlayPause = {},
                onSkipBackward = {},
                onSkipForward = {},
                onSelectSpeed = {},
                onSetVolumeBoost = {},
                onSelectTrimMode = {},
                onOpenPodcast = {},
            )
        }
    }
}
