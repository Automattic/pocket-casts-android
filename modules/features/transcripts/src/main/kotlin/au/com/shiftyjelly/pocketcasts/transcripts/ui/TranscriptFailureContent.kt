package au.com.shiftyjelly.pocketcasts.transcripts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PlayerColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColorsParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.ThemeColors
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType

internal data class TransciptFailureColor(
    val icon: Color,
    val text: Color,
    val button: Color,
    val buttonText: Color,
) {
    companion object {
        fun default(colors: ThemeColors) = TransciptFailureColor(
            icon = colors.primaryIcon02,
            text = colors.primaryText01,
            button = colors.primaryUi05,
            buttonText = colors.primaryText01,
        )

        fun player(colors: PlayerColors) = TransciptFailureColor(
            icon = colors.contrast02,
            text = colors.contrast02,
            button = colors.contrast05,
            buttonText = Color.White,
        )
    }
}

@Composable
internal fun TranscriptFailureContent(
    description: String,
    modifier: Modifier = Modifier,
    colors: TransciptFailureColor = TransciptFailureColor.default(MaterialTheme.theme.colors),
    buttonLabel: String? = null,
    onClickButton: (() -> Unit)? = null,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_warning),
            contentDescription = null,
            tint = colors.icon,
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        TextP40(
            text = description,
            color = colors.text,
            textAlign = TextAlign.Center,
            fontFamily = TranscriptTheme.RobotoSerifFontFamily,
        )
        if (buttonLabel != null) {
            Spacer(
                modifier = Modifier.height(20.dp),
            )
            CompositionLocalProvider(
                LocalRippleConfiguration provides RippleConfiguration(color = colors.buttonText),
            ) {
                Button(
                    onClick = { onClickButton?.invoke() },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(backgroundColor = colors.button),
                ) {
                    TextP40(
                        text = buttonLabel,
                        color = colors.buttonText,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun TranscriptFailureContentreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(theme) {
        Box(
            modifier = Modifier.padding(16.dp),
        ) {
            TranscriptFailureContent(
                description = "Sorry, but something went wrong while loading this transcript",
                buttonLabel = "Try again",
                onClickButton = {},
            )
        }
    }
}

@Preview
@Composable
private fun TranscriptFailureContentPlayerPreview(
    @PreviewParameter(PodcastColorsParameterProvider::class) podcastColors: PodcastColors,
) {
    AppTheme(ThemeType.ROSE) {
        CompositionLocalProvider(LocalPodcastColors provides podcastColors) {
            val transcriptTheme = rememberTranscriptTheme()
            Box(
                modifier = Modifier
                    .background(transcriptTheme.background)
                    .padding(16.dp),
            ) {
                TranscriptFailureContent(
                    description = "Sorry, but something went wrong while loading this transcript",
                    colors = transcriptTheme.failureColors,
                    buttonLabel = "Try again",
                    onClickButton = {},
                )
            }
        }
    }
}
