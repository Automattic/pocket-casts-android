package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import android.os.Build
import android.view.KeyEvent
import android.view.View
import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
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
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.FadedLazyColumn
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
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
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel.TransitionState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.TranscriptFormat
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.Util
import com.google.common.collect.ImmutableList
import com.kevinnzou.web.LoadingState
import com.kevinnzou.web.WebView
import com.kevinnzou.web.rememberWebViewNavigator
import com.kevinnzou.web.rememberWebViewState
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@kotlin.OptIn(ExperimentalMaterialApi::class)
@Composable
fun TranscriptPage(
    shelfSharedViewModel: ShelfSharedViewModel,
    transcriptViewModel: TranscriptViewModel,
    searchViewModel: TranscriptSearchViewModel,
    theme: Theme,
    modifier: Modifier = Modifier,
) {
    val uiState = transcriptViewModel.uiState.collectAsStateWithLifecycle()
    val transitionState = shelfSharedViewModel.transitionState.collectAsStateWithLifecycle(null)
    val searchState = searchViewModel.searchState.collectAsStateWithLifecycle()
    val refreshing = transcriptViewModel.isRefreshing.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullRefreshState(refreshing.value, {
        transcriptViewModel.parseAndLoadTranscript(pulledToRefresh = true)
    })
    val playerBackgroundColor = Color(theme.playerBackgroundColor(uiState.value.podcastAndEpisode?.podcast))
    val colors = TranscriptColors(playerBackgroundColor)
    Box(
        modifier = modifier
            .fillMaxSize()
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
                    transitionState = transitionState.value,
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
                        transcriptViewModel.parseAndLoadTranscript(retryOnFail = true)
                    },
                    colors = colors,
                )
            }
        }
    }

    LaunchedEffect(uiState.value.transcript?.episodeUuid, uiState.value.transcript?.type, transitionState.value) {
        if (transitionState.value is TransitionState.OpenTranscript) {
            transcriptViewModel.parseAndLoadTranscript()
        }
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
    transitionState: TransitionState?,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.backgroundColor()),
    ) {
        Box(
            modifier = if (state.showPaywall) {
                if (Build.VERSION.SDK_INT >= 31) {
                    Modifier
                        .blur(8.dp)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0f to Color.Black,
                                    0.2f to Color.Black,
                                    0.3f to Color.Transparent,
                                    1f to Color.Transparent,
                                ),
                                blendMode = BlendMode.DstOut,
                            )
                        }
                } else {
                    Modifier.alpha(0.1f)
                }
            } else {
                Modifier
            },
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
                    transitionState = transitionState,
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ScrollableTranscriptView(
    state: UiState.TranscriptLoaded,
    searchState: SearchUiState,
    transitionState: TransitionState?,
) {
    val lazyListState = rememberLazyListState()

    CompositionLocalProvider(
        LocalTextToolbar provides CustomTextToolbar(
            view = LocalView.current,
            customMenuItems = buildList {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    add(CustomMenuItemOption.Share)
                }
            },
            clipboardManager = LocalClipboardManager.current,
        ),
    ) {
        SelectionContainer {
            if (state.showAsWebPage) {
                TranscriptWebView(state, transitionState)
            } else {
                TranscriptItems(state, searchState, lazyListState)
            }
        }
    }

    ScrollToHighlightedTextEffect(state, searchState, lazyListState)
}

@Composable
private fun TranscriptWebView(
    state: UiState.TranscriptLoaded,
    transitionState: TransitionState?,
) {
    val webViewState = rememberWebViewState(state.transcript.url)
    val navigator = rememberWebViewNavigator()
    val lastLoadedUri = webViewState.lastLoadedUrl?.toUri()
    val transcriptUri = state.transcript.url.toUri()
    val isRootUrl = "${lastLoadedUri?.host}${lastLoadedUri?.path}" == "${transcriptUri.host}${transcriptUri.path}" // Ignore scheme http or https
    WebView(
        state = webViewState,
        navigator = navigator,
        onCreated = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.settings.isAlgorithmicDarkeningAllowed = true
            } else {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                    @Suppress("DEPRECATION")
                    WebSettingsCompat.setForceDark(it.settings, WebSettingsCompat.FORCE_DARK_ON)
                }
            }
            it.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            it.setOnKeyListener(
                View.OnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        if (keyCode == KeyEvent.KEYCODE_BACK && it.canGoBack() && !isRootUrl) {
                            it.goBack()
                            return@OnKeyListener true
                        }
                    }
                    false
                },
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding()),
    )
    if (webViewState.loadingState is LoadingState.Loading) {
        LoadingView(color = TranscriptColors.textColor())
    }
    LaunchedEffect(transitionState, webViewState.viewState) {
        if (!isRootUrl) navigator.navigateBack()
    }
}

@kotlin.OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TranscriptItems(
    state: UiState.TranscriptLoaded,
    searchState: SearchUiState,
    listState: LazyListState,
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val displayWidthPercent = if (Util.isTablet(LocalContext.current)) 0.8f else 1f
    val horizontalContentPadding = ((1 - displayWidthPercent) * screenWidthDp).dp / 2

    Column(
        modifier = Modifier.padding(horizontal = horizontalContentPadding),
    ) {
        if (state.transcript.isGenerated) {
            Text(
                text = stringResource(LR.string.transcript_generated_header),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = TranscriptColors.textColor(),
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .background(TranscriptColors.accentColor())
                    .width(48.dp)
                    .height(1.dp),
            )
        }

        FadedLazyColumn(
            state = listState,
            modifier = Modifier
                .padding(bottom = bottomPadding())
                .verticalScrollBar(
                    thumbColor = TranscriptColors.accentColor(),
                    scrollState = listState,
                    contentPadding = PaddingValues(bottom = TranscriptDefaults.ContentOffsetBottom),
                ),
        ) {
            item(contentType = "padding") {
                Spacer(
                    modifier = Modifier.height(16.dp),
                )
            }
            items(
                items = state.displayInfo.items,
                contentType = { "transcript" },
            ) { item ->
                TranscriptItem(
                    item = item,
                    searchState = searchState,
                )
            }
            item(contentType = "padding") {
                Spacer(
                    modifier = Modifier.height(16.dp),
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
private fun ScrollToHighlightedTextEffect(
    state: UiState.TranscriptLoaded,
    searchState: SearchUiState,
    lazyListState: LazyListState,
) {
    if (searchState.searchResultIndices.isNotEmpty()) {
        val density = LocalDensity.current
        val scrollToHighlightedTextOffset = density.run { scrollToHighlightedTextOffset().roundToPx() }

        LaunchedEffect(searchState.searchTerm, searchState.currentSearchIndex) {
            val displayItems = state.displayInfo.items
            val targetSearchResultIndexIndex = searchState.searchResultIndices[searchState.currentSearchIndex]
            displayItems
                .find { item -> targetSearchResultIndexIndex in item.startIndex until item.endIndex }
                ?.let { displayItemWithCurrentSearchText ->
                    lazyListState.animateScrollToItem(
                        index = displayItems.indexOf(displayItemWithCurrentSearchText),
                        scrollOffset = -scrollToHighlightedTextOffset,
                    )
                }
        }
    }
}

@Preview(name = "Phone")
@Composable
private fun TranscriptPhonePreview() {
    TranscriptContentPreview(searchState = SearchUiState())
}
@Preview(name = "Generated")
@Composable
private fun TranscriptGenereatedPreview() {
    TranscriptContentPreview(searchState = SearchUiState(), isGenerated = true)
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
    isGenerated: Boolean = false,
) {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        TranscriptContent(
            state = UiState.TranscriptLoaded(
                podcastAndEpisode = null,
                transcript = Transcript(
                    episodeUuid = "uuid",
                    type = TranscriptFormat.HTML.mimeType,
                    url = "url",
                    isGenerated = isGenerated,
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
                        DisplayItem("Lorem ipsum odor amet, consectetuer adipiscing elit.", false, 0, 52),
                        DisplayItem("Speaker 1", true, 0, 8),
                        DisplayItem("Sodales sem fusce elementum commodo risus purus auctor neque.", false, 53, 114),
                        DisplayItem("Maecenas fermentum senectus penatibus tenectus integer per vulputate tellus ted.", false, 115, 195),
                    ),
                ),
                isSubscriptionRequired = false,
            ),
            searchState = searchState,
            transitionState = null,
            colors = TranscriptColors(Color.Black),
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
                    isGenerated = false,
                ),
                cuesInfo = emptyList(),
                displayInfo = DisplayInfo(
                    text = "",
                    items = emptyList(),
                ),
                isSubscriptionRequired = false,
            ),
            searchState = SearchUiState(),
            transitionState = null,
            colors = TranscriptColors(Color.Black),
        )
    }
}
