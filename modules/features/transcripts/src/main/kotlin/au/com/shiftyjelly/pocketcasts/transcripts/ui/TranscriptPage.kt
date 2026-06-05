package au.com.shiftyjelly.pocketcasts.transcripts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.transcripts.TranscriptState
import au.com.shiftyjelly.pocketcasts.transcripts.TranscriptViewModel
import au.com.shiftyjelly.pocketcasts.transcripts.UiState
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.search.SearchCoordinates
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TranscriptPage(
    uiState: UiState,
    onClickClose: () -> Unit,
    onClickReload: () -> Unit,
    onUpdateSearchTerm: (String) -> Unit,
    onClearSearchTerm: () -> Unit,
    onSelectPreviousSearch: () -> Unit,
    onSelectNextSearch: () -> Unit,
    onShowSearchBar: () -> Unit,
    onHideSearchBar: () -> Unit,
    onClickSubscribe: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TranscriptViewModel? = null,
    fingerprintTimingManager: FingerprintTimingManager? = null,
    playbackManager: PlaybackManager? = null,
    toolbarPadding: PaddingValues = PaddingValues(0.dp),
    transcriptPadding: PaddingValues = PaddingValues(0.dp),
    paywallPadding: PaddingValues = PaddingValues(0.dp),
    showCloseButton: Boolean = true,
    toolbarTrailingContent: (@Composable (ToolbarColors) -> Unit)? = null,
    onHighlightText: (() -> Unit)? = null,
) {
    val theme = rememberTranscriptTheme()
    val listState = rememberLazyListState()
    var highlightState by remember { mutableStateOf(HighlightState()) }
    var hasInitiallyScrolled by remember { mutableStateOf(false) }
    var isAutoScrollSuppressed by remember { mutableStateOf(false) }
    val playbackState by remember(playbackManager) {
        playbackManager?.playbackStateFlow ?: flowOf(null)
    }.collectAsState(initial = null)
    val isPlaying = playbackState?.isPlaying == true
    val isSearching = uiState.searchState.isSearchOpen

    Box(modifier = modifier.background(theme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Toolbar(
                searchState = uiState.searchState,
                hideSearchBar = uiState.isPaywallVisible || !uiState.isTextTranscriptLoaded,
                showCloseButton = showCloseButton,
                onClickClose = onClickClose,
                onUpdateSearchTerm = onUpdateSearchTerm,
                onClearSearchTerm = onClearSearchTerm,
                onSelectPreviousSearch = onSelectPreviousSearch,
                onSelectNextSearch = onSelectNextSearch,
                onShowSearchBar = onShowSearchBar,
                onHideSearchBar = onHideSearchBar,
                colors = theme.toolbarColors,
                trailingContent = toolbarTrailingContent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(toolbarPadding),
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                val tapToSeekHandler: ((TranscriptEntry, Int) -> Unit)? =
                    if (uiState.isSyncedActive && viewModel != null) {
                        { entry, _ -> viewModel.seekToTranscriptEntry(entry) }
                    } else {
                        null
                    }

                TranscriptContent(
                    uiState = uiState,
                    listState = listState,
                    theme = theme,
                    onClickReload = onClickReload,
                    highlightState = highlightState,
                    onEntryClick = tapToSeekHandler,
                    onHighlightText = onHighlightText,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .padding(transcriptPadding),
                )

                if (uiState.isPaywallVisible) {
                    TranscriptsPaywall(
                        isFreeTrialAvailable = uiState.isFreeTrialAvailable,
                        onClickSubscribe = onClickSubscribe,
                        theme = theme,
                        contentPadding = paywallPadding,
                    )
                }
            }
        }

        if (
            FeatureFlag.isEnabled(Feature.SYNCED_TRANSCRIPTS) &&
            FeatureFlag.isEnabled(Feature.SYNCED_TRANSCRIPT_DEBUG) &&
            fingerprintTimingManager != null &&
            playbackManager != null
        ) {
            val debugBottomPadding = transcriptPadding.calculateBottomPadding() + 16.dp
            SyncDebugTimeline(
                fingerprintTimingManager = fingerprintTimingManager,
                playbackManager = playbackManager,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = debugBottomPadding),
            )
        }
    }

    ScrollToItemEffect(
        searchCoordinates = uiState.searchState.matches.selectedCoordinate,
        listState = listState,
    )

    HighlightEffect(
        uiState = uiState,
        fingerprintTimingManager = fingerprintTimingManager,
        playbackManager = playbackManager,
        onHighlightChange = { highlightState = it },
    )

    AutoScrollEffect(
        highlightIndex = highlightState.entryIndex,
        listState = listState,
        isSearching = isSearching,
        isPlaying = isPlaying,
        isAutoScrollSuppressed = isAutoScrollSuppressed,
        animate = hasInitiallyScrolled,
        onScroll = { hasInitiallyScrolled = true },
    )

    UserScrollDetectionEffect(
        listState = listState,
        onSuppressScroll = { isAutoScrollSuppressed = true },
        onResumeScroll = { isAutoScrollSuppressed = false },
    )

    KeepScreenOnEffect(keepOn = uiState.isSyncedActive)
}

@Composable
private fun TranscriptContent(
    uiState: UiState,
    listState: LazyListState,
    theme: TranscriptTheme,
    onClickReload: () -> Unit,
    onHighlightText: (() -> Unit)?,
    modifier: Modifier = Modifier,
    highlightState: HighlightState = HighlightState(),
    onEntryClick: ((TranscriptEntry, Int) -> Unit)? = null,
) {
    when (val transcriptState = uiState.transcriptState) {
        is TranscriptState.Loading -> {
            LoadingView(
                color = theme.primaryText,
                modifier = modifier,
            )
        }

        is TranscriptState.Loaded -> when (val transcript = transcriptState.transcript) {
            is Transcript.Text -> {
                TranscriptLines(
                    transcript = transcript,
                    isContentObscured = uiState.isPaywallVisible,
                    searchState = uiState.searchState,
                    highlightState = highlightState,
                    onEntryClick = onEntryClick,
                    state = listState,
                    theme = theme,
                    onHighlightText = onHighlightText,
                    modifier = modifier,
                )
            }

            is Transcript.Web -> {
                TranscriptWebView(
                    transcript = transcript,
                    theme = theme,
                    modifier = modifier,
                )
            }
        }

        is TranscriptState.Failure -> {
            TranscriptFailureContent(
                description = stringResource(LR.string.error_transcript_failed_to_load),
                colors = theme.failureColors,
                buttonLabel = stringResource(LR.string.try_again),
                onClickButton = onClickReload,
                modifier = modifier.fillMaxSize(),
            )
        }

        is TranscriptState.NoContent -> {
            TranscriptFailureContent(
                description = stringResource(LR.string.transcript_empty),
                colors = theme.failureColors,
                modifier = modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ScrollToItemEffect(
    searchCoordinates: SearchCoordinates?,
    listState: LazyListState,
) {
    val scrollOffset = LocalDensity.current.run { 64.dp.roundToPx() }
    if (searchCoordinates != null) {
        LaunchedEffect(searchCoordinates) {
            listState.animateScrollToItem(searchCoordinates.line, -scrollOffset)
        }
    }
}

@Composable
private fun HighlightEffect(
    uiState: UiState,
    fingerprintTimingManager: FingerprintTimingManager?,
    playbackManager: PlaybackManager?,
    onHighlightChange: (HighlightState) -> Unit,
) {
    if (fingerprintTimingManager == null || playbackManager == null) return

    val transcript = (uiState.transcriptState as? TranscriptState.Loaded)?.transcript as? Transcript.Text ?: return
    val isSyncedActive = uiState.isSyncedActive
    val latestOnHighlightChanged by rememberUpdatedState(onHighlightChange)

    val playbackState by remember(playbackManager) {
        playbackManager.playbackStateFlow
    }.collectAsState(initial = null)
    val isPlaying = playbackState?.isPlaying == true
    val isAdInProgress by remember(fingerprintTimingManager) {
        fingerprintTimingManager.isAdInProgress
    }.collectAsState()

    if (isPlaying && isSyncedActive && !isAdInProgress) {
        LaunchedEffect(transcript.entries) {
            var cachedIndex = 0
            while (true) {
                withFrameNanos { _ ->
                    val posMs = playbackState?.positionMs ?: return@withFrameNanos
                    val currentRefTime = fingerprintTimingManager.referenceTime(forPlaybackTimeMs = posMs) ?: return@withFrameNanos
                    val currentRefTimeMs = (currentRefTime * 1000).toLong()
                    val idx = TranscriptCueHelper.findCueIndex(transcript.entries, currentRefTimeMs, cachedIndex)
                    if (idx != null) {
                        cachedIndex = idx
                        val entry = transcript.entries[idx]
                        val wordIdx = if (entry is TranscriptEntry.Text && entry.words.isNotEmpty()) {
                            TranscriptCueHelper.findWordIndex(entry, currentRefTimeMs)
                        } else {
                            null
                        }
                        latestOnHighlightChanged(HighlightState(entryIndex = idx, wordIndex = wordIdx))
                    }
                }
            }
        }
    } else if (!isSyncedActive || isAdInProgress) {
        LaunchedEffect(isAdInProgress) { latestOnHighlightChanged(HighlightState()) }
    }
}

@Composable
private fun AutoScrollEffect(
    highlightIndex: Int?,
    listState: LazyListState,
    isSearching: Boolean,
    isPlaying: Boolean,
    isAutoScrollSuppressed: Boolean,
    animate: Boolean,
    onScroll: () -> Unit,
) {
    val latestOnScroll by rememberUpdatedState(onScroll)
    if (highlightIndex != null && isPlaying && !isSearching && !isAutoScrollSuppressed) {
        LaunchedEffect(highlightIndex) {
            if (highlightIndex >= listState.layoutInfo.totalItemsCount) return@LaunchedEffect
            val viewportHeight = listState.layoutInfo.viewportSize.height
            val scrollOffset = (viewportHeight * 0.3f).roundToInt()
            if (animate) {
                listState.animateScrollToItem(highlightIndex, -scrollOffset)
            } else {
                listState.scrollToItem(highlightIndex, -scrollOffset)
            }
            latestOnScroll()
        }
    }
}

@Composable
private fun UserScrollDetectionEffect(
    listState: LazyListState,
    onSuppressScroll: () -> Unit,
    onResumeScroll: () -> Unit,
) {
    val latestOnSuppressScroll by rememberUpdatedState(onSuppressScroll)
    val latestOnResumeScroll by rememberUpdatedState(onResumeScroll)
    LaunchedEffect(listState) {
        var resumeJob: Job? = null
        listState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> {
                    resumeJob?.cancel()
                    latestOnSuppressScroll()
                }

                is DragInteraction.Stop, is DragInteraction.Cancel -> {
                    resumeJob?.cancel()
                    resumeJob = launch {
                        delay(AUTO_SCROLL_BACK_DELAY_MS)
                        latestOnResumeScroll()
                    }
                }
            }
        }
    }
}

@Composable
private fun KeepScreenOnEffect(keepOn: Boolean) {
    val view = LocalView.current
    DisposableEffect(keepOn) {
        val previousKeepScreenOn = view.keepScreenOn
        view.keepScreenOn = keepOn
        onDispose {
            view.keepScreenOn = previousKeepScreenOn
        }
    }
}

internal data class HighlightState(
    val entryIndex: Int? = null,
    val wordIndex: Int? = null,
)

private const val AUTO_SCROLL_BACK_DELAY_MS = 5000L
