package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.TranscriptError
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.UiState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TranscriptPage(
    viewModel: TranscriptViewModel,
    theme: Theme,
) {
    val state = viewModel.uiState.collectAsState()
    when (state.value) {
        is UiState.Empty -> Unit

        is UiState.TranscriptFound -> {
            val transcriptFoundState = state.value as UiState.TranscriptFound
            val colors = DefaultColors(theme, transcriptFoundState.podcastAndEpisode?.podcast)
            LoadingView(Modifier.background(colors.backgroundColor()))
        }

        is UiState.TranscriptLoaded -> {
            val loadedState = state.value as UiState.TranscriptLoaded
            TranscriptContent(
                state = loadedState,
                colors = DefaultColors(theme, loadedState.podcastAndEpisode?.podcast),
            )
        }

        is UiState.Error -> {
            val errorState = state.value as UiState.Error
            TranscriptError(
                state = errorState,
                colors = DefaultColors(theme, errorState.podcastAndEpisode?.podcast),
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.parseAndLoadTranscript()
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun TranscriptContent(
    state: UiState.TranscriptLoaded,
    colors: DefaultColors,
) {
    val normalStyle = SpanStyle(
        fontSize = 16.sp,
        color = colors.textColor(),
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundColor()),
    ) {
        Text(
            buildAnnotatedString {
                withStyle(style = ParagraphStyle(lineHeight = 30.sp)) {
                    (0..<state.cuesWithTimingSubtitle.eventTimeCount).forEach {
                        val time = state.cuesWithTimingSubtitle.getEventTime(it)
                        state.cuesWithTimingSubtitle.getCues(time).forEach { cue ->
                            cue?.let {
                                withStyle(style = normalStyle) {
                                    append(cue.text)
                                }
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        )
    }
}

@Composable
private fun TranscriptError(
    state: UiState.Error,
    colors: DefaultColors,
) {
    val errorMessage = when (val error = state.error) {
        is TranscriptError.NotSupported ->
            stringResource(LR.string.error_transcript_format_not_supported, error.format)

        is TranscriptError.FailedToLoad ->
            stringResource(LR.string.error_transcript_failed_to_load)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundColor())
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier = Modifier
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
                    .padding(16.dp),
            ) {
                TextH30(
                    text = stringResource(LR.string.error),
                    color = colors.titleColor(),
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
