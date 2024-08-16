package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptDefaults.TranscriptColors
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.TranscriptError
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.UiState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.TranscriptFormat
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TranscriptError(
    state: UiState.Error,
    colors: TranscriptColors,
    onRetry: () -> Unit,
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
            .background(colors.backgroundColor())
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = TranscriptDefaults.ContentOffsetBottom),
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_warning),
                contentDescription = null,
                tint = TranscriptColors.iconColor().copy(alpha = 0.5f),
            )
            TextP40(
                text = errorMessage,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
                color = TranscriptColors.textColor(),
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 16.dp),
                shape = RoundedCornerShape(40.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = TranscriptColors.contentColor()),
            ) {
                TextP40(
                    text = stringResource(LR.string.try_again),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W400,
                )
            }
        }
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
            onRetry = {},
            colors = TranscriptColors(Color.Black),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
