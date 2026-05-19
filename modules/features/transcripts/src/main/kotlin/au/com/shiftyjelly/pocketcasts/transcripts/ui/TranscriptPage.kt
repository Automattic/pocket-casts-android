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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.map
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.transcripts.TranscriptState
import au.com.shiftyjelly.pocketcasts.transcripts.UiState
import au.com.shiftyjelly.pocketcasts.utils.search.SearchCoordinates
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
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
    onShowTranscript: (Transcript) -> Unit,
    onShowTranscriptPaywall: (Transcript) -> Unit,
    modifier: Modifier = Modifier,
    fingerprintTimingManager: FingerprintTimingManager? = null,
    playbackManager: PlaybackManager? = null,
    toolbarPadding: PaddingValues = PaddingValues(0.dp),
    transcriptPadding: PaddingValues = PaddingValues(0.dp),
    paywallPadding: PaddingValues = PaddingValues(0.dp),
    toolbarTrailingContent: (@Composable (ToolbarColors) -> Unit)? = null,
) {
    val theme = rememberTranscriptTheme()
    val listState = rememberLazyListState()
    var highlightIndex by remember { mutableStateOf<Int?>(null) }
    var hasInitiallyScrolled by remember { mutableStateOf(false) }
    var isAutoScrollSuppressed by remember { mutableStateOf(false) }
    val isSearching = uiState.searchState.isSearchOpen

    Column(
        modifier = modifier.background(theme.background),
    ) {
        Toolbar(
            searchState = uiState.searchState,
            hideSearchBar = uiState.isPaywallVisible || !uiState.isTextTranscriptLoaded,
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
            val tapToSeekHandler: ((TranscriptEntry, Int) -> Unit)? = if (uiState.isSyncedActive && fingerprintTimingManager != null && playbackManager != null) {
                { entry, _ ->
                    val textEntry = entry as? TranscriptEntry.Text
                    if (textEntry != null && textEntry.startTimeMs >= 0) {
                        val refTimeSec = textEntry.startTimeMs / 1000.0
                        val seekTimeMs = fingerprintTimingManager.playbackTimeMs(forReferenceTime = refTimeSec)
                        if (seekTimeMs != null) {
                            playbackManager.seekToTimeMs(seekTimeMs)
                        }
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
                highlightIndex = highlightIndex,
                onEntryClick = tapToSeekHandler,
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

    ScrollToItemEffect(
        searchCoordinates = uiState.searchState.matches.selectedCoordinate,
        listState = listState,
    )

    ShowTranscriptEffect(
        uiState = uiState,
        onShowTranscript = onShowTranscript,
        onShowTranscriptPaywall = onShowTranscriptPaywall,
    )

    // Frame-rate highlight updates
    HighlightEffect(
        uiState = uiState,
        fingerprintTimingManager = fingerprintTimingManager,
        playbackManager = playbackManager,
        onHighlightChanged = { highlightIndex = it },
    )

    // Auto-scroll to highlighted cue
    AutoScrollEffect(
        highlightIndex = highlightIndex,
        listState = listState,
        isSearching = isSearching,
        isAutoScrollSuppressed = isAutoScrollSuppressed,
        animate = hasInitiallyScrolled,
        onScrolled = { hasInitiallyScrolled = true },
    )

    // Detect user scrolling and suppress auto-scroll for 5s
    UserScrollDetectionEffect(
        listState = listState,
        onScrollSuppressed = { isAutoScrollSuppressed = true },
        onScrollResumed = { isAutoScrollSuppressed = false },
    )
}

@Composable
private fun TranscriptContent(
    uiState: UiState,
    listState: LazyListState,
    theme: TranscriptTheme,
    onClickReload: () -> Unit,
    modifier: Modifier = Modifier,
    highlightIndex: Int? = null,
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
                    highlightIndex = highlightIndex,
                    onEntryClick = onEntryClick,
                    state = listState,
                    theme = theme,
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
private fun ShowTranscriptEffect(
    uiState: UiState,
    onShowTranscript: (Transcript) -> Unit,
    onShowTranscriptPaywall: (Transcript) -> Unit,
) {
    val transcript = (uiState.transcriptState as? TranscriptState.Loaded)?.transcript
    if (transcript != null) {
        val isPaywallVisible = uiState.isPaywallVisible
        LaunchedEffect(transcript, isPaywallVisible, onShowTranscript, onShowTranscriptPaywall) {
            // This delay is necessary due to how transcript loading works in the player.
            //
            // We trigger the page open animation and transcript loading at the same time.
            // This means that the callbacks from this effect can be invoked with
            // lingering state from the previously shown transcript, followed by
            // the new incoming state. This can result in issues such as analytics overreporting.
            //
            // Since loading happens almost immediately when the page opens,
            // adding a small delay prevents the situation described above,
            // because the effect will be cancelled before it runs.
            //
            // We could potentially clear the transcript state after
            // closing the page, but this would trigger animations,
            // resulting in a flicker-like visual experience.
            delay(100)

            if (isPaywallVisible) {
                onShowTranscriptPaywall(transcript)
            } else {
                onShowTranscript(transcript)
            }
        }
    }
}

@Composable
private fun HighlightEffect(
    uiState: UiState,
    fingerprintTimingManager: FingerprintTimingManager?,
    playbackManager: PlaybackManager?,
    onHighlightChanged: (Int?) -> Unit,
) {
    if (fingerprintTimingManager == null || playbackManager == null) return

    val transcript = (uiState.transcriptState as? TranscriptState.Loaded)?.transcript as? Transcript.Text ?: return
    val isSyncedActive = uiState.isSyncedActive
    val isPlaying by remember {
        playbackManager.playbackStateFlow.map { it.isPlaying }
    }.collectAsState(initial = playbackManager.isPlaying())

    if (isPlaying && isSyncedActive) {
        LaunchedEffect(transcript.entries) {
            var cachedIndex = 0
            while (true) {
                withFrameNanos { _ ->
                    val posMs = playbackManager.playbackStateRelay.blockingFirst().positionMs
                    val refTime = fingerprintTimingManager.referenceTime(forPlaybackTimeMs = posMs) ?: return@withFrameNanos
                    val refTimeMs = (refTime * 1000).toLong()
                    val idx = findCueIndex(transcript.entries, refTimeMs, cachedIndex)
                    if (idx != null) cachedIndex = idx
                    onHighlightChanged(idx)
                }
            }
        }
    } else if (!isSyncedActive) {
        LaunchedEffect(Unit) { onHighlightChanged(null) }
    }
}

@Composable
private fun AutoScrollEffect(
    highlightIndex: Int?,
    listState: LazyListState,
    isSearching: Boolean,
    isAutoScrollSuppressed: Boolean,
    animate: Boolean,
    onScrolled: () -> Unit,
) {
    if (highlightIndex != null && !isSearching && !isAutoScrollSuppressed) {
        LaunchedEffect(highlightIndex) {
            val viewportHeight = listState.layoutInfo.viewportSize.height
            val scrollOffset = (viewportHeight * 0.3f).roundToInt()
            if (animate) {
                listState.animateScrollToItem(highlightIndex, -scrollOffset)
            } else {
                listState.scrollToItem(highlightIndex, -scrollOffset)
            }
            onScrolled()
        }
    }
}

@Composable
private fun UserScrollDetectionEffect(
    listState: LazyListState,
    onScrollSuppressed: () -> Unit,
    onScrollResumed: () -> Unit,
) {
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> {
                    onScrollSuppressed()
                }
                is DragInteraction.Stop, is DragInteraction.Cancel -> {
                    delay(AUTO_SCROLL_BACK_DELAY_MS)
                    onScrollResumed()
                }
            }
        }
    }
}

private fun findCueIndex(
    entries: List<TranscriptEntry>,
    refTimeMs: Long,
    cachedIndex: Int,
): Int? {
    if (entries.isEmpty()) return null
    val cached = cachedIndex.coerceAtMost(entries.size - 1)

    val cachedEntry = entries[cached]
    if (cachedEntry is TranscriptEntry.Text && cachedEntry.startTimeMs >= 0 &&
        refTimeMs >= cachedEntry.startTimeMs && refTimeMs <= cachedEntry.endTimeMs
    ) {
        return cached
    }

    // Forward scan from cached position
    for (i in (cached + 1) until entries.size) {
        val entry = entries[i]
        if (entry is TranscriptEntry.Text && entry.startTimeMs >= 0) {
            if (entry.startTimeMs > refTimeMs) break
            if (refTimeMs >= entry.startTimeMs && refTimeMs <= entry.endTimeMs) return i
        }
    }

    // Backward scan
    for (i in (cached - 1) downTo 0) {
        val entry = entries[i]
        if (entry is TranscriptEntry.Text && entry.startTimeMs >= 0) {
            if (refTimeMs >= entry.startTimeMs && refTimeMs <= entry.endTimeMs) return i
        }
    }

    return null
}

private const val AUTO_SCROLL_BACK_DELAY_MS = 5000L
