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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    HighlightEffect(
        uiState = uiState,
        fingerprintTimingManager = fingerprintTimingManager,
        playbackManager = playbackManager,
        onHighlightChange = { highlightIndex = it },
    )

    AutoScrollEffect(
        highlightIndex = highlightIndex,
        listState = listState,
        isSearching = isSearching,
        isAutoScrollSuppressed = isAutoScrollSuppressed,
        animate = hasInitiallyScrolled,
        onScroll = { hasInitiallyScrolled = true },
    )

    UserScrollDetectionEffect(
        listState = listState,
        onSuppressScroll = { isAutoScrollSuppressed = true },
        onResumeScroll = { isAutoScrollSuppressed = false },
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
private fun HighlightEffect(
    uiState: UiState,
    fingerprintTimingManager: FingerprintTimingManager?,
    playbackManager: PlaybackManager?,
    onHighlightChange: (Int?) -> Unit,
) {
    if (fingerprintTimingManager == null || playbackManager == null) return

    val transcript = (uiState.transcriptState as? TranscriptState.Loaded)?.transcript as? Transcript.Text ?: return
    val isSyncedActive = uiState.isSyncedActive
    val latestOnHighlightChanged by rememberUpdatedState(onHighlightChange)

    val playbackState by remember {
        playbackManager.playbackStateFlow
    }.collectAsState(initial = null)
    val isPlaying = playbackState?.isPlaying == true

    if (isPlaying && isSyncedActive) {
        LaunchedEffect(transcript.entries) {
            var cachedIndex = 0
            while (true) {
                withFrameNanos { _ ->
                    val posMs = playbackState?.positionMs ?: return@withFrameNanos
                    val currentRefTime = fingerprintTimingManager.referenceTime(forPlaybackTimeMs = posMs) ?: return@withFrameNanos
                    val currentRefTimeMs = (currentRefTime * 1000).toLong()
                    val idx = findCueIndex(transcript.entries, currentRefTimeMs, cachedIndex)
                    if (idx != null) {
                        cachedIndex = idx
                        latestOnHighlightChanged(idx)
                    }
                }
            }
        }
    } else if (!isSyncedActive) {
        LaunchedEffect(Unit) { latestOnHighlightChanged(null) }
    }
}

@Composable
private fun AutoScrollEffect(
    highlightIndex: Int?,
    listState: LazyListState,
    isSearching: Boolean,
    isAutoScrollSuppressed: Boolean,
    animate: Boolean,
    onScroll: () -> Unit,
) {
    val latestOnScroll by rememberUpdatedState(onScroll)
    if (highlightIndex != null && !isSearching && !isAutoScrollSuppressed) {
        LaunchedEffect(highlightIndex) {
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

    val scanLimit = 10
    val nearbyResult = findCueNearby(entries, refTimeMs, cached, scanLimit)
    if (nearbyResult != null) return nearbyResult

    return findCueBinarySearch(entries, refTimeMs)
}

private fun findCueNearby(
    entries: List<TranscriptEntry>,
    refTimeMs: Long,
    cached: Int,
    scanLimit: Int,
): Int? {
    for (i in (cached + 1) until minOf(cached + 1 + scanLimit, entries.size)) {
        val entry = entries[i]
        if (entry is TranscriptEntry.Text && entry.startTimeMs >= 0) {
            if (entry.startTimeMs > refTimeMs) break
            if (refTimeMs >= entry.startTimeMs && refTimeMs <= entry.endTimeMs) return i
        }
    }
    for (i in (cached - 1) downTo maxOf(cached - scanLimit, 0)) {
        val entry = entries[i]
        if (entry is TranscriptEntry.Text && entry.startTimeMs >= 0) {
            if (refTimeMs >= entry.startTimeMs && refTimeMs <= entry.endTimeMs) return i
        }
    }
    return null
}

private fun findCueBinarySearch(
    entries: List<TranscriptEntry>,
    refTimeMs: Long,
): Int? {
    var lo = 0
    var hi = entries.size - 1
    while (lo <= hi) {
        val mid = (lo + hi) / 2
        val entry = entries[mid]
        if (entry is TranscriptEntry.Text && entry.startTimeMs >= 0) {
            when {
                refTimeMs < entry.startTimeMs -> hi = mid - 1
                refTimeMs > entry.endTimeMs -> lo = mid + 1
                else -> return mid
            }
        } else {
            // Walk outward from mid to find the nearest timed Text entry for comparison.
            val timedIdx = findNearestTimedEntry(entries, mid, lo, hi)
            if (timedIdx == null) {
                break
            } else {
                val timed = entries[timedIdx] as TranscriptEntry.Text
                when {
                    refTimeMs < timed.startTimeMs -> hi = timedIdx - 1
                    refTimeMs > timed.endTimeMs -> lo = timedIdx + 1
                    else -> return timedIdx
                }
            }
        }
    }
    return findClosestTimedEntry(entries, refTimeMs, lo.coerceIn(0, entries.size - 1))
}

private fun findClosestTimedEntry(
    entries: List<TranscriptEntry>,
    refTimeMs: Long,
    around: Int,
): Int? {
    var bestIndex: Int? = null
    var bestDistance = Long.MAX_VALUE
    val scanRadius = 5
    val start = maxOf(0, around - scanRadius)
    val end = minOf(entries.size - 1, around + scanRadius)
    for (i in start..end) {
        val entry = entries[i]
        if (entry is TranscriptEntry.Text && entry.startTimeMs >= 0) {
            val dist = minOf(
                abs(refTimeMs - entry.startTimeMs),
                abs(refTimeMs - entry.endTimeMs),
            )
            if (dist < bestDistance) {
                bestDistance = dist
                bestIndex = i
            }
        }
    }
    return if (bestDistance <= NEAREST_CUE_THRESHOLD_MS) bestIndex else null
}

private fun findNearestTimedEntry(
    entries: List<TranscriptEntry>,
    mid: Int,
    lo: Int,
    hi: Int,
): Int? {
    var left = mid - 1
    var right = mid + 1
    while (left >= lo || right <= hi) {
        if (right <= hi) {
            val entry = entries[right]
            if (entry is TranscriptEntry.Text && entry.startTimeMs >= 0) return right
            right++
        }
        if (left >= lo) {
            val entry = entries[left]
            if (entry is TranscriptEntry.Text && entry.startTimeMs >= 0) return left
            left--
        }
    }
    return null
}

private const val AUTO_SCROLL_BACK_DELAY_MS = 5000L

private const val NEAREST_CUE_THRESHOLD_MS = 5000L
