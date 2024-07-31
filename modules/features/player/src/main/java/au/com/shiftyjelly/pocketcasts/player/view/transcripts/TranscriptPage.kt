package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import android.content.res.Configuration
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.extensions.FadeDirection
import au.com.shiftyjelly.pocketcasts.compose.extensions.gradientBackground
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.compose.text.HtmlText
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.TranscriptError
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.UiState
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel.TransitionState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.TranscriptFormat
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
fun TranscriptPage(
    playerViewModel: PlayerViewModel,
    transcriptViewModel: TranscriptViewModel,
    theme: Theme,
    modifier: Modifier,
) {
    val uiState = transcriptViewModel.uiState.collectAsStateWithLifecycle()
    val transitionState = playerViewModel.transitionState.collectAsStateWithLifecycle(null)
    when (uiState.value) {
        is UiState.Empty -> {
            val emptyState = uiState.value as UiState.Empty
            val colors = DefaultColors(theme, emptyState.podcastAndEpisode?.podcast)
            EmptyView(Modifier.background(colors.backgroundColor()))
        }

        is UiState.TranscriptFound -> {
            val transcriptFoundState = uiState.value as UiState.TranscriptFound
            val colors = DefaultColors(theme, transcriptFoundState.podcastAndEpisode?.podcast)
            LoadingView(Modifier.background(colors.backgroundColor()))
        }

        is UiState.TranscriptLoaded -> {
            val loadedState = uiState.value as UiState.TranscriptLoaded
            TranscriptContent(
                state = loadedState,
                colors = DefaultColors(theme, loadedState.podcastAndEpisode?.podcast),
                modifier = modifier,
            )
        }

        is UiState.Error -> {
            val errorState = uiState.value as UiState.Error
            TranscriptError(
                state = errorState,
                colors = DefaultColors(theme, errorState.podcastAndEpisode?.podcast),
                modifier = modifier,
            )
        }
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

@OptIn(UnstableApi::class)
@Composable
private fun TranscriptContent(
    state: UiState.TranscriptLoaded,
    colors: DefaultColors,
    modifier: Modifier,
) {
    val defaultTextStyle = SpanStyle(fontSize = 16.sp, color = colors.textColor())
    val configuration = LocalConfiguration.current
    val bottomPadding = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 0.dp else 125.dp

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
                (0 until eventTimeCount).forEach { index ->
                    getCues(getEventTime(index)).forEach { cue ->
                        withStyle(style = defaultTextStyle) { append(cue.text) }
                        append(" ")
                    }
                }
                append(blankLines)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.backgroundColor()),
    ) {
        if (state.transcript.type == TranscriptFormat.HTML.mimeType) {
            /* Display html content using Android text view.
               Html rendering in Compose text view is available in Compose 1.7.0 beta which is not yet production ready: https://rb.gy/ev7182 */
            HtmlText(
                html = displayString.toString(),
                color = colors.textColor(),
                textStyleResId = UR.style.H40,
                modifier = modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = bottomPadding)
                    .verticalScroll(rememberScrollState()),
            )
        } else {
            Text(
                displayString,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = bottomPadding)
                    .verticalScroll(rememberScrollState()),
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
    val theme: Theme,
    val podcast: Podcast?,
) {
    @Composable
    fun backgroundColor() =
        Color(theme.playerBackgroundColor(podcast))

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
