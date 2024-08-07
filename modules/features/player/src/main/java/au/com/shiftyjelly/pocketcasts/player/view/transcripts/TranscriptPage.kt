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
import androidx.compose.ui.text.ParagraphStyle
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
import au.com.shiftyjelly.pocketcasts.compose.text.HtmlText
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.compose.toolbars.textselection.CustomMenuItemOption
import au.com.shiftyjelly.pocketcasts.compose.toolbars.textselection.CustomTextToolbar
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.TranscriptError
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.UiState
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel.TransitionState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.TranscriptFormat
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.google.common.collect.ImmutableList
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@kotlin.OptIn(ExperimentalMaterialApi::class)
@Composable
fun TranscriptPage(
    playerViewModel: PlayerViewModel,
    transcriptViewModel: TranscriptViewModel,
    theme: Theme,
    modifier: Modifier,
) {
    val uiState = transcriptViewModel.uiState.collectAsStateWithLifecycle()
    val transitionState = playerViewModel.transitionState.collectAsStateWithLifecycle(null)
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
            ScrollableTranscriptTextView(
                state,
                colors,
                bottomPadding,
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
private fun ScrollableTranscriptTextView(
    state: UiState.TranscriptLoaded,
    colors: DefaultColors,
    bottomPadding: Dp,
) {
    val defaultTextStyle = SpanStyle(fontSize = 16.sp, color = colors.textColor())
    /* Blank lines are appended to add content padding */
    val blankLines = if (state.transcript.type == TranscriptFormat.HTML.mimeType) {
        "<br><br>"
    } else {
        "\n\n"
    }
    val displayString = buildAnnotatedString {
        withStyle(style = ParagraphStyle(lineHeight = 30.sp)) {
            with(state.cuesWithTimingSubtitle) {
                append(blankLines)
                (0 until count()).forEach { index ->
                    get(index).cues.forEach { cue ->
                        withStyle(style = defaultTextStyle) { append(cue.text) }
                        append(" ")
                    }
                }
                append(blankLines)
            }
        }
    }
    val scrollState = rememberScrollState()
    val textModifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(bottom = bottomPadding)
        .verticalScroll(scrollState)
        .verticalScrollBar(
            thumbColor = colors.textColor(),
            scrollState = scrollState,
            contentPadding = PaddingValues(top = 64.dp, bottom = 80.dp),
        )

    if (state.transcript.type == TranscriptFormat.HTML.mimeType) {
        /* Display html content using Android text view.
               Html rendering in Compose text view is available in Compose 1.7.0 beta which is not yet production ready: https://rb.gy/ev7182 */
        HtmlText(
            html = displayString.toString(),
            color = colors.textColor(),
            textStyleResId = UR.style.H40,
            selectable = true,
            modifier = textModifier,
        )
    } else {
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
                Text(
                    text = displayString,
                    modifier = textModifier,
                )
            }
        }
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

@OptIn(UnstableApi::class)
@Preview(name = "Dark")
@Composable
private fun TranscriptContentPreview() {
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
                                "Lorem ipsum odor amet, consectetuer adipiscing elit. Sodales sem fusce elementum commodo risus purus auctor neque. Maecenas fermentum senectus penatibus senectus integer per vulputate tellus sed. Laoreet justo orci luctus venenatis taciti lobortis sapien. Torquent quis dignissim curabitur magna molestie lectus pretium litora. Urna sodales rutrum posuere fusce velit turpis sollicitudin iaculis. Imperdiet turpis natoque vehicula cursus quisque congue.<br>" +
                                    "<br>" +
                                    "Quis etiam torquent feugiat penatibus curabitur. Facilisi inceptos egestas dolor mauris eget; rutrum facilisis nam. Ipsum mollis auctor mollis libero facilisi, sed posuere tristique lectus. Morbi erat suscipit eu feugiat nisi mauris. Convallis nostra condimentum est turpis ornare egestas lorem euismod at. Est nec eleifend leo proin vel hendrerit. Sem ipsum duis nam bibendum faucibus vestibulum class. Leo iaculis magna dignissim sit tristique porttitor dapibus non.<br>" +
                                    "<br>" +
                                    "Dis etiam suspendisse rhoncus, a class nisi porttitor. Ornare velit imperdiet natoque elit lacinia suscipit. Feugiat phasellus vestibulum sapien posuere rhoncus. Massa hendrerit purus taciti elit, maecenas non lobortis. Potenti class condimentum consectetur convallis, lacus habitasse praesent. Potenti risus mi neque volutpat vivamus taciti.<br>",

                            ).build(),
                        ),
                        0,
                        0,
                    ),
                ),
            ),
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
            ),
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
