package au.com.shiftyjelly.pocketcasts.transcripts.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.transcripts.TranscriptState
import au.com.shiftyjelly.pocketcasts.transcripts.UiState
import au.com.shiftyjelly.pocketcasts.utils.search.SearchCoordinates
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
    toolbarPadding: PaddingValues = PaddingValues(0.dp),
    transcriptPadding: PaddingValues = PaddingValues(0.dp),
    paywallPadding: PaddingValues = PaddingValues(0.dp),
    toolbarTrailingContent: (@Composable (ToolbarColors) -> Unit)? = null,
) {
    val theme = rememberTranscriptTheme()
    val listState = rememberLazyListState()

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
            TranscriptContent(
                uiState = uiState,
                listState = listState,
                theme = theme,
                onClickReload = onClickReload,
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
}

@Composable
private fun TranscriptContent(
    uiState: UiState,
    listState: LazyListState,
    theme: TranscriptTheme,
    onClickReload: () -> Unit,
    modifier: Modifier = Modifier,
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
