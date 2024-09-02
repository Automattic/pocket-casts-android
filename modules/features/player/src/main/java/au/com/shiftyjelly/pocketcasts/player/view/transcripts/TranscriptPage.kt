package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import android.os.Build
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.extensions.FadeDirection
import au.com.shiftyjelly.pocketcasts.compose.extensions.gradientBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.verticalScrollBar
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.compose.toolbars.textselection.CustomMenuItemOption
import au.com.shiftyjelly.pocketcasts.compose.toolbars.textselection.CustomTextToolbar
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptCuesInfo
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptDefaults.TranscriptColors
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptDefaults.TranscriptFontFamily
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptDefaults.bottomPadding
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptDefaults.scrollToHighlightedTextOffset
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptSearchViewModel.SearchUiState
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.DisplayInfo
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.DisplayItem
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.UiState
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel.TransitionState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.TranscriptFormat
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.Util
import com.google.common.collect.ImmutableList
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@kotlin.OptIn(ExperimentalMaterialApi::class)
@Composable
fun TranscriptPage(
    playerViewModel: PlayerViewModel,
    transcriptViewModel: TranscriptViewModel,
    searchViewModel: TranscriptSearchViewModel,
    theme: Theme,
    modifier: Modifier,
) {
    val uiState = transcriptViewModel.uiState.collectAsStateWithLifecycle()
    val transitionState = playerViewModel.transitionState.collectAsStateWithLifecycle(null)
    val searchState = searchViewModel.searchState.collectAsStateWithLifecycle()
    val refreshing = transcriptViewModel.isRefreshing.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullRefreshState(refreshing.value, {
        transcriptViewModel.parseAndLoadTranscript(isTranscriptViewOpen = true, pulledToRefresh = true)
    })
    val playerBackgroundColor = Color(theme.playerBackgroundColor(uiState.value.podcastAndEpisode?.podcast))
    val colors = TranscriptColors(playerBackgroundColor)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .pullRefresh(pullRefreshState),
    ) {
        when (uiState.value) {
            is UiState.Empty -> {
                EmptyView(Modifier.background(colors.backgroundColor()))
            }

            is UiState.TranscriptFound -> {
                LoadingView(
                    modifier = Modifier.background(colors.backgroundColor()),
                    color = TranscriptColors.textColor(),
                )
            }

            is UiState.TranscriptLoaded -> {
                val loadedState = uiState.value as UiState.TranscriptLoaded

                TranscriptContent(
                    state = loadedState,
                    searchState = searchState.value,
                    colors = colors,
                    modifier = modifier,
                )

                PullRefreshIndicator(
                    refreshing = refreshing.value,
                    state = pullRefreshState,
                    backgroundColor = TranscriptColors.contentColor(),
                    contentColor = TranscriptColors.iconColor(),
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }

            is UiState.Error -> {
                val errorState = uiState.value as UiState.Error
                TranscriptError(
                    state = errorState,
                    onRetry = {
                        transcriptViewModel.parseAndLoadTranscript(
                            isTranscriptViewOpen = true,
                            retryOnFail = true,
                        )
                    },
                    colors = colors,
                    modifier = modifier,
                )
            }
        }
    }

    LaunchedEffect(uiState.value.transcript?.episodeUuid, uiState.value.transcript?.type, transitionState.value) {
        transcriptViewModel.parseAndLoadTranscript(transitionState.value is TransitionState.OpenTranscript)
    }

    if (uiState.value is UiState.TranscriptLoaded) {
        val state = uiState.value as UiState.TranscriptLoaded
        LaunchedEffect(state.displayInfo.text) {
            searchViewModel.setSearchInput(state.displayInfo.text, state.podcastAndEpisode)
        }
    }
}

@Composable
private fun EmptyView(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize())
}

@Composable
private fun TranscriptContent(
    state: UiState.TranscriptLoaded,
    searchState: SearchUiState,
    colors: TranscriptColors,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.backgroundColor()),
    ) {
        if (state.isTranscriptEmpty) {
            TextP40(
                text = stringResource(LR.string.transcript_empty),
                color = TranscriptColors.textColor(),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 60.dp),
            )
        } else {
            ScrollableTranscriptView(
                state = state,
                searchState = searchState,
            )
        }

        GradientView(
            baseColor = colors.backgroundColor(),
            modifier = Modifier
                .align(Alignment.TopCenter),
            fadeDirection = FadeDirection.TopToBottom,
        )

        GradientView(
            baseColor = colors.backgroundColor(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding()),
            fadeDirection = FadeDirection.BottomToTop,
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ScrollableTranscriptView(
    state: UiState.TranscriptLoaded,
    searchState: SearchUiState,
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val displayWidthPercent = if (Util.isTablet(LocalContext.current)) 0.8f else 1f
    val horizontalContentPadding = ((1 - displayWidthPercent) * screenWidthDp).dp / 2

    val scrollState = rememberLazyListState()
    val scrollableContentModifier = Modifier
        .padding(bottom = bottomPadding())
        .verticalScrollBar(
            thumbColor = TranscriptColors.textColor(),
            scrollState = scrollState,
            contentPadding = PaddingValues(top = TranscriptDefaults.ContentOffsetTop, bottom = TranscriptDefaults.ContentOffsetBottom),
        )

    val customMenu = buildList {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            add(CustomMenuItemOption.Share)
        }
    }
    CompositionLocalProvider(
        LocalTextToolbar provides CustomTextToolbar(
            LocalView.current,
            customMenu,
            LocalClipboardManager.current,
        ),
    ) {
        SelectionContainer {
            LazyColumn(
                state = scrollState,
                modifier = scrollableContentModifier,
                contentPadding = PaddingValues(
                    start = horizontalContentPadding,
                    end = horizontalContentPadding,
                    top = 64.dp,
                    bottom = 80.dp,
                ),
            ) {
                items(state.displayInfo.items) { item ->
                    TranscriptItem(
                        item = item,
                        searchState = searchState,
                    )
                }
            }
        }
    }

    // Scroll to highlighted text
    if (searchState.searchResultIndices.isNotEmpty()) {
        val density = LocalDensity.current
        val scrollToHighlightedTextOffset = density.run { scrollToHighlightedTextOffset().roundToPx() }
        LaunchedEffect(searchState.searchTerm, searchState.currentSearchIndex) {
            val displayItems = state.displayInfo.items
            val targetSearchResultIndexIndex = searchState.searchResultIndices[searchState.currentSearchIndex]
            displayItems.find { item ->
                targetSearchResultIndexIndex in item.startIndex until item.endIndex
            }?.let { displayItemWithCurrentSearchText ->
                scrollState.animateScrollToItem(
                    displayItems.indexOf(displayItemWithCurrentSearchText),
                    scrollOffset = -scrollToHighlightedTextOffset,
                )
            }
        }
    }
}

@Composable
private fun TranscriptItem(
    item: DisplayItem,
    searchState: SearchUiState,
) {
    val defaultTextStyle = SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.W500, fontFamily = TranscriptFontFamily, color = TranscriptColors.textColor())
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .padding(bottom = if (item.isSpeaker) 8.dp else 16.dp)
            .padding(top = if (item.isSpeaker) 16.dp else 0.dp),
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(defaultTextStyle) { append(item.text) }
                if (item.isSpeaker) {
                    addStyle(
                        style = defaultTextStyle.copy(fontSize = 12.sp),
                        start = 0,
                        end = item.text.length,
                    )
                }
                if (searchState.searchTerm.isNotEmpty()) {
                    // Highlight search occurrences
                    searchState.searchResultIndices
                        .filter { searchResultIndex -> searchResultIndex in item.startIndex until item.endIndex }
                        .forEach { searchResultIndex ->
                            val style = if (searchState.searchResultIndices.indexOf(searchResultIndex) == searchState.currentSearchIndex) {
                                TranscriptDefaults.SearchOccurrenceSelectedSpanStyle
                            } else {
                                TranscriptDefaults.SearchOccurrenceDefaultSpanStyle
                            }

                            val start = searchResultIndex - item.startIndex
                            addStyle(
                                style = if (item.isSpeaker) style.copy(fontSize = 12.sp) else style,
                                start = start,
                                end = start + searchState.searchTerm.length,
                            )
                        }
                }
            },
        )
    }
}

@Composable
private fun GradientView(
    baseColor: Color,
    modifier: Modifier = Modifier,
    fadeDirection: FadeDirection,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height((screenHeight * 0.1).dp)
            .gradientBackground(
                baseColor = baseColor,
                colorStops = listOf(
                    Color.Black,
                    Color.Transparent,
                ),
                direction = fadeDirection,
            ),
    )
}

@Preview(name = "Phone")
@Composable
private fun TranscriptPhonePreview() {
    TranscriptContentPreview(searchState = SearchUiState())
}

@Preview(name = "PortraitFoldable", device = Devices.PortraitFoldable)
@Composable
private fun TranscriptPortraitFoldableContentPreview() {
    TranscriptContentPreview(searchState = SearchUiState())
}

@Preview(name = "Tablet", device = Devices.PortraitTablet)
@Composable
private fun TranscriptTabletContentPreview() {
    TranscriptContentPreview(searchState = SearchUiState())
}

@Preview(name = "Phone")
@Composable
private fun TranscriptWithSearchContentPreview() {
    TranscriptContentPreview(
        searchState = SearchUiState(
            searchTerm = "se",
            searchResultIndices = listOf(26, 61, 134),
            currentSearchIndex = 0,
        ),
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun TranscriptContentPreview(
    searchState: SearchUiState,
) {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        TranscriptContent(
            state = UiState.TranscriptLoaded(
                podcastAndEpisode = null,
                transcript = Transcript(
                    episodeUuid = "uuid",
                    type = TranscriptFormat.HTML.mimeType,
                    url = "url",
                ),
                cuesInfo = ImmutableList.of(
                    TranscriptCuesInfo(
                        CuesWithTiming(
                            ImmutableList.of(
                                Cue.Builder().setText(
                                    "Speaker 1",
                                ).build(),
                            ),
                            0,
                            0,
                        ),
                    ),
                ),
                displayInfo = DisplayInfo(
                    text = "",
                    items = listOf(
                        DisplayItem("Speaker 1", true, 0, 8),
                        DisplayItem("Lorem ipsum odor amet, consectetuer adipiscing elit.", false, 0, 52),
                        DisplayItem("Sodales sem fusce elementum commodo risus purus auctor neque.", false, 53, 114),
                        DisplayItem("Maecenas fermentum senectus penatibus tenectus integer per vulputate tellus ted.", false, 115, 195),
                    ),
                ),
            ),
            searchState = searchState,
            colors = TranscriptColors(Color.Black),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@OptIn(UnstableApi::class)
@Preview(name = "Phone")
@Composable
private fun TranscriptEmptyContentPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        TranscriptContent(
            state = UiState.TranscriptLoaded(
                podcastAndEpisode = null,
                transcript = Transcript(
                    episodeUuid = "uuid",
                    type = TranscriptFormat.HTML.mimeType,
                    url = "url",
                ),
                cuesInfo = emptyList(),
                displayInfo = DisplayInfo(
                    text = "",
                    items = emptyList(),
                ),
            ),
            searchState = SearchUiState(),
            colors = TranscriptColors(Color.Black),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
