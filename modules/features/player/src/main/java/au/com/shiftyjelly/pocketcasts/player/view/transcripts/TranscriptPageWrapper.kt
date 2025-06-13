package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.buttons.CloseButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.IconButtonSmall
import au.com.shiftyjelly.pocketcasts.compose.components.SearchBar
import au.com.shiftyjelly.pocketcasts.compose.components.SearchBarDefaults
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptDefaults.TranscriptColors
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptSearchViewModel.SearchUiState
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.TranscriptState
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel.TransitionState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val SearchBarMaxWidth = 500.dp
private val SearchBarHeight = 43.dp
private val SearchViewCornerRadius = 38.dp
private val SearchBarIconColor = Color.Gray.copy(alpha = 0.8f)
private val SearchBarPlaceholderColor = SearchBarIconColor

@Composable
fun TranscriptPageWrapper(
    shelfSharedViewModel: ShelfSharedViewModel,
    transcriptViewModel: TranscriptViewModel,
    searchViewModel: TranscriptSearchViewModel,
    onClickSubscribe: () -> Unit,
) {
    AppTheme(Theme.ThemeType.DARK) {
        val transitionState by shelfSharedViewModel.transitionState.collectAsStateWithLifecycle(null)
        val uiState by transcriptViewModel.uiState.collectAsStateWithLifecycle()
        val searchState by searchViewModel.searchState.collectAsStateWithLifecycle()
        val searchQuery by searchViewModel.searchQueryFlow.collectAsStateWithLifecycle()

        val configuration = LocalConfiguration.current

        var showPaywall by remember { mutableStateOf(false) }
        var showSearch by remember { mutableStateOf(false) }
        var expandSearch by remember { mutableStateOf(false) }
        when (transitionState) {
            is TransitionState.CloseTranscript -> {
                if (expandSearch) {
                    expandSearch = false
                    searchViewModel.onSearchDone()
                }
            }

            else -> Unit
        }

        val playerColors = MaterialTheme.theme.rememberPlayerColors()
        val playerBackgroundColor = playerColors?.background01 ?: Color(0xFF3D3D3D)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(configuration.screenHeightDp.dp)
                .background(playerBackgroundColor),
        ) {
            TranscriptToolbar(
                onCloseClick = {
                    shelfSharedViewModel.closeTranscript()
                },
                showSearch = showSearch,
                onSearchDoneClicked = {
                    expandSearch = false
                    searchViewModel.onSearchDone()
                },
                onSearchClicked = {
                    expandSearch = true
                    searchViewModel.onSearchButtonClicked()
                },
                searchText = searchQuery,
                searchState = searchState,
                onSearchCleared = { searchViewModel.onSearchCleared() },
                onSearchPreviousClicked = { searchViewModel.onSearchPrevious() },
                onSearchNextClicked = { searchViewModel.onSearchNext() },
                onSearchQueryChanged = searchViewModel::onSearchQueryChanged,
                expandSearch = expandSearch,
            )

            Box(
                modifier = Modifier.weight(1f),
            ) {
                TranscriptPage(
                    shelfSharedViewModel = shelfSharedViewModel,
                    transcriptViewModel = transcriptViewModel,
                    searchViewModel = searchViewModel,
                )

                if (showPaywall) {
                    TranscriptsPaywall(
                        onClickSubscribe = onClickSubscribe,
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    )
                }
            }
        }

        LaunchedEffect(uiState.showSearch) {
            showSearch = uiState.showSearch

            if (!showSearch) {
                expandSearch = false
            }
        }

        LaunchedEffect(uiState.showPaywall, transitionState) {
            showPaywall = uiState.showPaywall

            when (val state = transitionState) {
                is TransitionState.OpenTranscript -> {
                    if (state.showPlayerControls) {
                        if (showPaywall) {
                            shelfSharedViewModel.openTranscript(showPlayerControls = false)
                        }
                    } else {
                        if (!showPaywall) {
                            shelfSharedViewModel.openTranscript(showPlayerControls = true)
                        }
                    }
                }
                is TransitionState.CloseTranscript, null -> Unit
            }
        }

        val isTranscriptOpen = (transitionState is TransitionState.OpenTranscript)
        val transcript = (uiState.transcriptState as? TranscriptState.Loaded)?.transcript

        LaunchedEffect(showPaywall, isTranscriptOpen, transcript?.episodeUuid, transcript?.url) {
            if (!showPaywall && isTranscriptOpen && transcript != null) {
                transcriptViewModel.track(
                    AnalyticsEvent.TRANSCRIPT_SHOWN,
                    mapOf(
                        "type" to transcript.type.analyticsValue,
                        "show_as_webpage" to (transcript is Transcript.Web).toString(),
                    ),
                )
            }
            if (showPaywall) {
                transcriptViewModel.track(AnalyticsEvent.TRANSCRIPT_GENERATED_PAYWALL_SHOWN)
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
@Composable
fun TranscriptToolbar(
    onCloseClick: () -> Unit,
    showSearch: Boolean,
    onSearchDoneClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    onSearchCleared: () -> Unit,
    onSearchPreviousClicked: () -> Unit,
    onSearchNextClicked: () -> Unit,
    searchText: String,
    searchState: SearchUiState,
    onSearchQueryChanged: (String) -> Unit,
    expandSearch: Boolean,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    AppTheme(Theme.ThemeType.LIGHT) { // Makes search bar always white for any theme
        Box(
            contentAlignment = Alignment.TopEnd,
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(),
        ) {
            val transition = updateTransition(expandSearch, label = "Searchbar transition")
            CompositionLocalProvider(LocalRippleConfiguration provides ToolbarRippleConfiguration) {
                CloseButton(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp),
                    onClick = onCloseClick,
                    tintColor = TranscriptColors.iconColor(),
                    contentDescription = stringResource(LR.string.transcript_close),
                )
            }

            if (showSearch) {
                transition.AnimatedVisibility(
                    visible = { !it },
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    IconButton(
                        onClick = onSearchClicked,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .background(Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(SearchViewCornerRadius)),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(LR.string.transcript_search),
                            tint = Color.White,
                        )
                    }
                }

                transition.AnimatedVisibility(
                    visible = { it },
                    enter = expandHorizontally(),
                    exit = shrinkHorizontally(targetWidth = { 50 }) + fadeOut(),
                ) {
                    SearchBar(
                        text = searchText,
                        leadingIcon = {
                            SearchBarLeadingIcons(
                                onDoneClicked = onSearchDoneClicked,
                            )
                        },
                        trailingIcon = {
                            SearchBarTrailingIcons(
                                text = searchText,
                                searchState = searchState,
                                onSearchCleared = onSearchCleared,
                                onPrevious = onSearchPreviousClicked,
                                onNext = onSearchNextClicked,
                            )
                        },
                        placeholder = stringResource(LR.string.search),
                        onTextChanged = onSearchQueryChanged,
                        onSearch = {},
                        cornerRadius = SearchViewCornerRadius,
                        modifier = Modifier
                            .width(SearchBarMaxWidth)
                            .height(SearchBarHeight)
                            .focusRequester(focusRequester)
                            .padding(start = 85.dp, end = 16.dp),
                        colors = SearchBarDefaults.colors(
                            leadingIconColor = SearchBarIconColor,
                            trailingIconColor = SearchBarIconColor,
                            disabledTrailingIconColor = SearchBarIconColor.copy(alpha = 0.7f),
                            placeholderColor = SearchBarPlaceholderColor,
                        ),
                        contentPadding = TextFieldDefaults.textFieldWithoutLabelPadding(
                            top = 0.dp,
                            bottom = 0.dp,
                            start = 0.dp,
                        ),
                    )
                }
            }

            LaunchedEffect(expandSearch) {
                if (expandSearch) {
                    focusRequester.requestFocus()
                } else {
                    focusManager.clearFocus()
                }
            }
        }
    }
}

@Composable
private fun SearchBarLeadingIcons(
    onDoneClicked: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButtonSmall(
            onClick = {
                onDoneClicked()
            },
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(LR.string.done),
            )
        }
    }
}

@Composable
private fun SearchBarTrailingIcons(
    text: String,
    searchState: SearchUiState,
    onSearchCleared: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (text.isNotEmpty()) {
            Text(
                text = searchState.searchOccurrencesText,
                color = SearchBarIconColor,
            )
            IconButtonSmall(
                onClick = {
                    onSearchCleared()
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_cancel),
                    contentDescription = stringResource(LR.string.cancel),
                )
            }
        }

        IconButtonSmall(
            onClick = onPrevious,
            enabled = searchState.prevNextArrowButtonsEnabled,
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = stringResource(LR.string.go_to_previous),
            )
        }
        IconButtonSmall(
            onClick = onNext,
            enabled = searchState.prevNextArrowButtonsEnabled,
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(LR.string.go_to_next),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
    }
}

@OptIn(ExperimentalMaterialApi::class)
private val ToolbarRippleConfiguration = RippleConfiguration(
    color = Color.White,
    rippleAlpha = RippleAlpha(
        pressedAlpha = 0.12f,
        focusedAlpha = 0.12f,
        draggedAlpha = 0.08f,
        hoveredAlpha = 0.04f,
    ),
)

@Preview("Collapsed search bar", heightDp = 100)
@Composable
private fun TranscriptToolbarPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        TranscriptToolbar(
            onCloseClick = {},
            showSearch = true,
            onSearchDoneClicked = {},
            onSearchClicked = {},
            onSearchCleared = {},
            onSearchPreviousClicked = {},
            onSearchNextClicked = {},
            searchText = "",
            searchState = SearchUiState(),
            onSearchQueryChanged = {},
            expandSearch = false,
        )
    }
}

@Preview("Expanded search bar", heightDp = 100)
@Composable
private fun TranscriptToolbarExpandedSearchPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        TranscriptToolbar(
            onCloseClick = {},
            showSearch = true,
            onSearchDoneClicked = {},
            onSearchClicked = {},
            onSearchCleared = {},
            onSearchPreviousClicked = {},
            onSearchNextClicked = {},
            searchText = "",
            searchState = SearchUiState(),
            onSearchQueryChanged = {},
            expandSearch = true,
        )
    }
}
