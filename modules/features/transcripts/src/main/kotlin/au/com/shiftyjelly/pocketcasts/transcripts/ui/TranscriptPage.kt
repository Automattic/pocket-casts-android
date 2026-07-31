package au.com.shiftyjelly.pocketcasts.transcripts.ui

import android.os.SystemClock
import android.widget.Toast
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.transcripts.TranscriptMessage
import au.com.shiftyjelly.pocketcasts.transcripts.TranscriptState
import au.com.shiftyjelly.pocketcasts.transcripts.TranscriptViewModel
import au.com.shiftyjelly.pocketcasts.transcripts.UiState
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.search.SearchCoordinates
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.milliseconds
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

    val syncableEpisodeUuid = uiState.transcriptEpisodeUuid.takeIf { uiState.isTextTranscriptLoaded }
    DisposableEffect(fingerprintTimingManager, syncableEpisodeUuid) {
        if (syncableEpisodeUuid != null) {
            fingerprintTimingManager?.onTranscriptShown(syncableEpisodeUuid)
        }
        onDispose {
            if (syncableEpisodeUuid != null) {
                fingerprintTimingManager?.onTranscriptDismissed(syncableEpisodeUuid)
            }
        }
    }

    var highlightState by remember { mutableStateOf(HighlightState()) }
    var hasInitiallyScrolled by remember { mutableStateOf(false) }
    var isAutoScrollSuppressed by remember { mutableStateOf(false) }
    var pendingSeek by remember { mutableStateOf<PendingTapSeek?>(null) }
    var forceScrollIndex by remember { mutableStateOf<Int?>(null) }
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
                val tapScope = rememberCoroutineScope()
                val tapToSeekHandler: ((TranscriptEntry, Int) -> Unit)? =
                    if (FeatureFlag.isEnabled(Feature.SYNCED_TRANSCRIPTS) && uiState.isTapToSeekAvailable && viewModel != null) {
                        { entry, index ->
                            val applySeek = { seekTarget: Int ->
                                // Hold the tapped row lit until fingerprinting catches up to the seek.
                                highlightState = HighlightState(entryIndex = index)
                                pendingSeek = PendingTapSeek(positionMs = seekTarget, entryIndex = index)
                                if (!isPlaying) {
                                    forceScrollIndex = index
                                }
                            }
                            when (val result = viewModel.seekToTranscriptEntry(entry)) {
                                is TranscriptViewModel.TapSeekResult.Seeked -> applySeek(result.positionMs)

                                TranscriptViewModel.TapSeekResult.Resolving -> {
                                    // Light the row as immediate feedback while the bounded resolve runs.
                                    // The null-target hold keeps the frame loop from overwriting it.
                                    val previousHighlight = highlightState
                                    highlightState = HighlightState(entryIndex = index)
                                    pendingSeek = PendingTapSeek(positionMs = null, entryIndex = index)
                                    tapScope.launch {
                                        var seekTarget: Int? = null
                                        try {
                                            seekTarget = viewModel.resolveAndSeekToEntry(entry)
                                        } finally {
                                            // Also restores the highlight when a user seek cancels the resolve.
                                            val target = seekTarget
                                            if (target != null) {
                                                applySeek(target)
                                            } else {
                                                pendingSeek = pendingSeek?.takeUnless { it.positionMs == null && it.entryIndex == index }
                                                if (highlightState.entryIndex == index) {
                                                    highlightState = previousHighlight
                                                }
                                            }
                                        }
                                    }
                                }

                                TranscriptViewModel.TapSeekResult.Unavailable -> Unit
                            }
                        }
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
        pendingSeek = pendingSeek,
        onConsumePendingSeek = { pendingSeek = null },
        onHighlightChange = { highlightState = it },
    )

    AutoScrollEffect(
        highlightIndex = highlightState.entryIndex,
        listState = listState,
        isSearching = isSearching,
        isPlaying = isPlaying,
        isAutoScrollSuppressed = isAutoScrollSuppressed,
        animate = hasInitiallyScrolled,
        forceScrollIndex = forceScrollIndex,
        onConsumeForceScroll = { forceScrollIndex = null },
        onScroll = { hasInitiallyScrolled = true },
    )

    UserScrollDetectionEffect(
        listState = listState,
        onSuppressScroll = { isAutoScrollSuppressed = true },
        onResumeScroll = { manualScrollDurationMs ->
            isAutoScrollSuppressed = false
            if (uiState.isSyncedActive) {
                viewModel?.trackAutoScrollResumed(manualScrollDurationMs)
            }
        },
    )

    KeepScreenOnEffect(keepOn = uiState.isSyncedActive)

    TranscriptMessageEffect(viewModel = viewModel)
}

@Composable
private fun TranscriptMessageEffect(viewModel: TranscriptViewModel?) {
    if (viewModel == null) return
    val context = LocalContext.current
    val tapToSeekUnavailableMessage = stringResource(LR.string.transcript_tap_to_seek_streaming_unavailable)
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            when (message) {
                TranscriptMessage.TapToSeekStreamingUnavailable -> {
                    Toast.makeText(context, tapToSeekUnavailableMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
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
    pendingSeek: PendingTapSeek?,
    onConsumePendingSeek: () -> Unit,
    onHighlightChange: (HighlightState) -> Unit,
) {
    if (fingerprintTimingManager == null || playbackManager == null) return

    val transcript = (uiState.transcriptState as? TranscriptState.Loaded)?.transcript as? Transcript.Text ?: return
    val isSyncedActive = uiState.isSyncedActive
    val latestOnHighlightChanged by rememberUpdatedState(onHighlightChange)
    val latestPendingSeek by rememberUpdatedState(pendingSeek)
    val latestOnConsumePendingSeek by rememberUpdatedState(onConsumePendingSeek)

    val playbackState by remember(playbackManager) {
        playbackManager.playbackStateFlow
    }.collectAsState(initial = null)
    val isPlaying = playbackState?.isPlaying == true

    val cueIndexHolder = remember(transcript.entries) { intArrayOf(0) }

    // A new transcript makes any held tap stale; branch flaps below must not drop it.
    LaunchedEffect(transcript.entries) {
        latestOnConsumePendingSeek()
    }

    if (isPlaying && isSyncedActive) {
        LaunchedEffect(transcript.entries) {
            cueIndexHolder[0] = 0
            var wasHighlighting = false
            var settledForSeek: Int? = null
            if (latestPendingSeek == null) {
                latestOnHighlightChanged(HighlightState())
            }
            val episode = playbackManager.getCurrentEpisode()
            while (true) {
                withFrameNanos { }
                val posMs = if (episode != null) {
                    playbackManager.getCurrentTimeMs(episode)
                } else {
                    playbackState?.positionMs ?: continue
                }
                val outcome = resolveHighlight(transcript.entries, posMs, fingerprintTimingManager, cueIndexHolder[0])

                val pending = latestPendingSeek
                if (pending != null) {
                    // Hold the tapped row until the target is known, the seek settles, and
                    // resolution reaches it.
                    val targetMs = pending.positionMs
                    if (targetMs != null && settledForSeek != targetMs &&
                        TranscriptCueHelper.isSeekSettled(posMs, targetMs)
                    ) {
                        settledForSeek = targetMs
                    }
                    val settled = targetMs != null && settledForSeek == targetMs
                    val reachedRow = TranscriptCueHelper.hasReachedTappedRow(outcome, pending.entryIndex)
                    if (settled && reachedRow) {
                        latestOnConsumePendingSeek()
                        settledForSeek = null
                    } else {
                        wasHighlighting = true
                        continue
                    }
                }

                when (outcome) {
                    is HighlightOutcome.Show -> {
                        cueIndexHolder[0] = outcome.entryIndex
                        latestOnHighlightChanged(HighlightState(entryIndex = outcome.entryIndex, wordIndex = outcome.wordIndex))
                        wasHighlighting = true
                    }

                    HighlightOutcome.Clear -> {
                        if (wasHighlighting) {
                            latestOnHighlightChanged(HighlightState())
                            wasHighlighting = false
                        }
                    }

                    HighlightOutcome.Keep -> Unit
                }
            }
        }
    } else if (isSyncedActive) {
        // Paused but synced: no frame loop, so recompute once per position change. A held tap
        // stays lit while the position sits at its target; seeking elsewhere releases it.
        LaunchedEffect(transcript.entries, playbackState?.positionMs) {
            val posMs = playbackState?.positionMs ?: return@LaunchedEffect
            val pending = latestPendingSeek
            if (pending != null) {
                if (!TranscriptCueHelper.isHeldTapStale(posMs, pending.positionMs)) {
                    return@LaunchedEffect
                }
                latestOnConsumePendingSeek()
            }
            when (val outcome = resolveHighlight(transcript.entries, posMs, fingerprintTimingManager, cueIndexHolder[0])) {
                is HighlightOutcome.Show -> {
                    cueIndexHolder[0] = outcome.entryIndex
                    latestOnHighlightChanged(HighlightState(entryIndex = outcome.entryIndex, wordIndex = outcome.wordIndex))
                }

                HighlightOutcome.Clear -> latestOnHighlightChanged(HighlightState())

                HighlightOutcome.Keep -> Unit
            }
        }
    } else {
        LaunchedEffect(Unit) { latestOnHighlightChanged(HighlightState()) }
    }
}

/**
 * Maps the current playback position to a highlight outcome. Returns [HighlightOutcome.Clear]
 * when playback is off matched content (ads / unmatched / outside the mapped range), otherwise
 * delegates the cue decision to [TranscriptCueHelper.resolveHighlight].
 */
private fun resolveHighlight(
    entries: List<TranscriptEntry>,
    posMs: Int,
    fingerprintTimingManager: FingerprintTimingManager,
    cachedIndex: Int,
): HighlightOutcome {
    val refTime = fingerprintTimingManager.matchedReferenceTime(forPlaybackTimeMs = posMs)
        ?: return HighlightOutcome.Clear
    // Round, not truncate: truncating lands before a cue boundary and resolves to the prior cue.
    val refTimeMs = (refTime * 1000).roundToLong()
    return TranscriptCueHelper.resolveHighlight(entries, refTimeMs, cachedIndex)
}

@Composable
private fun AutoScrollEffect(
    highlightIndex: Int?,
    listState: LazyListState,
    isSearching: Boolean,
    isPlaying: Boolean,
    isAutoScrollSuppressed: Boolean,
    animate: Boolean,
    forceScrollIndex: Int?,
    onConsumeForceScroll: () -> Unit,
    onScroll: () -> Unit,
) {
    val latestOnScroll by rememberUpdatedState(onScroll)
    val latestOnConsumeForceScroll by rememberUpdatedState(onConsumeForceScroll)
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

    if (forceScrollIndex != null && !isSearching) {
        LaunchedEffect(forceScrollIndex) {
            if (forceScrollIndex < listState.layoutInfo.totalItemsCount) {
                val viewportHeight = listState.layoutInfo.viewportSize.height
                val scrollOffset = (viewportHeight * 0.3f).roundToInt()
                listState.animateScrollToItem(forceScrollIndex, -scrollOffset)
            }
            latestOnConsumeForceScroll()
        }
    }
}

@Composable
private fun UserScrollDetectionEffect(
    listState: LazyListState,
    onSuppressScroll: () -> Unit,
    onResumeScroll: (manualScrollDurationMs: Long?) -> Unit,
) {
    val latestOnSuppressScroll by rememberUpdatedState(onSuppressScroll)
    val latestOnResumeScroll by rememberUpdatedState(onResumeScroll)
    LaunchedEffect(listState) {
        var resumeJob: Job? = null
        var dragStartMs: Long? = null
        listState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> {
                    resumeJob?.cancel()
                    dragStartMs = SystemClock.elapsedRealtime()
                    latestOnSuppressScroll()
                }

                is DragInteraction.Stop, is DragInteraction.Cancel -> {
                    val manualScrollDurationMs = dragStartMs?.let { SystemClock.elapsedRealtime() - it }
                    dragStartMs = null
                    resumeJob?.cancel()
                    resumeJob = launch {
                        delay(AUTO_SCROLL_BACK_DELAY_MS.milliseconds)
                        latestOnResumeScroll(manualScrollDurationMs)
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

internal data class PendingTapSeek(
    // Null while the bounded resolve is still computing the target position.
    val positionMs: Int?,
    val entryIndex: Int,
)

private const val AUTO_SCROLL_BACK_DELAY_MS = 5000L
