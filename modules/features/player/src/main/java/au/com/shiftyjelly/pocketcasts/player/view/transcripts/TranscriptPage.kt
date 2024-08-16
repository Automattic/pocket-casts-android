package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import android.content.res.Configuration
import android.os.Build
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
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
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.extensions.FadeDirection
import au.com.shiftyjelly.pocketcasts.compose.extensions.gradientBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.verticalScrollBar
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.compose.toolbars.textselection.CustomMenuItemOption
import au.com.shiftyjelly.pocketcasts.compose.toolbars.textselection.CustomTextToolbar
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptSearchViewModel.SearchUiState
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.DisplayInfo
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.DisplayItem
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.TranscriptError
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.UiState
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel.TransitionState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.TranscriptFormat
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.google.common.collect.ImmutableList
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val ContentOffsetTop = 64.dp
private val ContentOffsetBottom = 80.dp
private val SearchOccurrenceDefaultSpanStyle = SpanStyle(fontSize = 16.sp, background = Color.DarkGray, color = Color.White)
private val SearchOccurrenceSelectedSpanStyle = SpanStyle(fontSize = 16.sp, background = Color.White, color = Color.Black)

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
        transcriptViewModel.parseAndLoadTranscript(isTranscriptViewOpen = true, forceRefresh = true)
    })
    val playerBackgroundColor = Color(theme.playerBackgroundColor(uiState.value.podcastAndEpisode?.podcast))
    val colors = DefaultColors(playerBackgroundColor)
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
                LoadingView(Modifier.background(colors.backgroundColor()))
            }

            is UiState.TranscriptLoaded -> {
                val loadedState = uiState.value as UiState.TranscriptLoaded

                TranscriptContent(
                    state = loadedState,
                    searchState = searchState.value,
                    colors = colors,
                    modifier = modifier,
                )
            }

            is UiState.Error -> {
                val errorState = uiState.value as UiState.Error
                TranscriptError(
                    state = errorState,
                    colors = colors,
                    modifier = modifier,
                )
            }
        }
        PullRefreshIndicator(refreshing.value, pullRefreshState, Modifier.align(Alignment.TopCenter))
    }

    LaunchedEffect(uiState.value.transcript?.episodeUuid + transitionState.value) {
        transcriptViewModel.parseAndLoadTranscript(transitionState.value is TransitionState.OpenTranscript)
    }

    if (uiState.value is UiState.TranscriptLoaded) {
        val state = uiState.value as UiState.TranscriptLoaded
        LaunchedEffect(state.displayInfo.text) {
            searchViewModel.setSearchSourceText(state.displayInfo.text)
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
    colors: DefaultColors,
    modifier: Modifier,
) {
    val configuration = LocalConfiguration.current
    val bottomPadding = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 0.dp else 125.dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.backgroundColor()),
    ) {
        if (state.isTranscriptEmpty) {
            TextP40(
                text = stringResource(LR.string.transcript_empty),
                color = colors.textColor(),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 60.dp),
            )
        } else {
            ScrollableTranscriptView(
                state = state,
                searchState = searchState,
                colors = colors,
                bottomPadding = bottomPadding,
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
                .padding(bottom = bottomPadding),
            fadeDirection = FadeDirection.BottomToTop,
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ScrollableTranscriptView(
    state: UiState.TranscriptLoaded,
    searchState: SearchUiState,
    colors: DefaultColors,
    bottomPadding: Dp,
) {
    val scrollState = rememberLazyListState()
    val scrollableContentModifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(bottom = bottomPadding)
        .verticalScrollBar(
            thumbColor = colors.textColor(),
            scrollState = scrollState,
            contentPadding = PaddingValues(top = ContentOffsetTop, bottom = ContentOffsetBottom),
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
                contentPadding = PaddingValues(top = 64.dp, bottom = 80.dp),
            ) {
                items(state.displayInfo.items) { item ->
                    TranscriptItem(
                        colors = colors,
                        item = item,
                        searchState = searchState,
                    )
                }
            }
        }
    }
}

@Composable
private fun TranscriptItem(
    colors: DefaultColors,
    item: DisplayItem,
    searchState: SearchUiState,
) {
    val defaultTextStyle = SpanStyle(fontSize = 16.sp, color = colors.textColor())
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(defaultTextStyle) { append(item.text) }
                if (searchState.searchTerm.isNotEmpty()) {
                    // Highlight search occurrences
                    searchState.searchResultIndices
                        .filter { searchResultIndex -> searchResultIndex in item.startIndex until item.endIndex }
                        .forEach { searchResultIndex ->
                            val style = if (searchState.searchResultIndices.indexOf(searchResultIndex) == searchState.currentSearchIndex) {
                                SearchOccurrenceSelectedSpanStyle
                            } else {
                                SearchOccurrenceDefaultSpanStyle
                            }
                            val start = searchResultIndex - item.startIndex
                            addStyle(
                                style = style,
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

@Composable
private fun TranscriptError(
    state: UiState.Error,
    colors: DefaultColors,
    modifier: Modifier,
) {
    val errorMessage = when (val error = state.error) {
        is TranscriptError.NotSupported ->
            stringResource(LR.string.error_transcript_format_not_supported, error.format)

        is TranscriptError.NoNetwork ->
            stringResource(LR.string.error_no_network)

        is TranscriptError.FailedToParse ->
            stringResource(LR.string.error_transcript_failed_to_parse)

        is TranscriptError.FailedToLoad ->
            stringResource(LR.string.error_transcript_failed_to_load)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 32.dp)
            .background(colors.backgroundColor())
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(
                    color = colors.contentColor(),
                    shape = RoundedCornerShape(size = 4.dp),
                ),
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                TextH30(
                    text = stringResource(LR.string.error),
                    color = colors.titleColor(),
                    textAlign = TextAlign.Center,
                )
                TextP40(
                    text = errorMessage,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                    color = colors.textColor(),
                )
            }
        }
    }
}

private data class DefaultColors(
    val playerBackgroundColor: Color,
) {
    @Composable
    fun backgroundColor() =
        playerBackgroundColor

    @Composable
    fun contentColor() =
        MaterialTheme.theme.colors.playerContrast06

    @Composable
    fun textColor() =
        MaterialTheme.theme.colors.playerContrast02

    @Composable
    fun titleColor() =
        MaterialTheme.theme.colors.playerContrast01
}

@Preview(name = "Dark")
@Composable
private fun TranscriptWithoutSearchContentPreview() {
    TranscriptContentPreview(searchState = SearchUiState())
}

@Preview(name = "Dark")
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
                cuesWithTimingSubtitle =
                ImmutableList.of(
                    CuesWithTiming(
                        ImmutableList.of(
                            Cue.Builder().setText(
                                "Lorem ipsum odor amet, consectetuer adipiscing elit. Sodales sem fusce elementum commodo risus purus auctor neque. Maecenas fermentum senectus penatibus senectus integer per vulputate tellus sed.",
                            ).build(),
                        ),
                        0,
                        0,
                    ),
                ),
                displayInfo = DisplayInfo(
                    text = "",
                    items = listOf(
                        DisplayItem("Lorem ipsum odor amet, consectetuer adipiscing elit.", 0, 52),
                        DisplayItem("Sodales sem fusce elementum commodo risus purus auctor neque.", 53, 114),
                        DisplayItem("Maecenas fermentum senectus penatibus tenectus integer per vulputate tellus ted.", 115, 195),
                    ),
                ),
            ),
            searchState = searchState,
            colors = DefaultColors(Color.Black),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@OptIn(UnstableApi::class)
@Preview(name = "Dark")
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
                cuesWithTimingSubtitle = emptyList(),
                displayInfo = DisplayInfo(
                    text = "",
                    items = emptyList(),
                ),
            ),
            searchState = SearchUiState(),
            colors = DefaultColors(Color.Black),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(name = "Dark")
@Composable
private fun ErrorDarkPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        TranscriptError(
            state = UiState.Error(
                error = TranscriptError.NotSupported(TranscriptFormat.HTML.mimeType),
                transcript = Transcript(
                    episodeUuid = "uuid",
                    type = TranscriptFormat.HTML.mimeType,
                    url = "url",
                ),
            ),
            colors = DefaultColors(Color.Black),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
