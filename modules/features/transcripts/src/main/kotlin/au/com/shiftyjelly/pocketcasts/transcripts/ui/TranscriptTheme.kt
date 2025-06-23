package au.com.shiftyjelly.pocketcasts.transcripts.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import au.com.shiftyjelly.pocketcasts.compose.PlayerColors
import au.com.shiftyjelly.pocketcasts.compose.ThemeColors
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.R

internal data class TranscriptTheme(
    val background: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val secondaryElement: Color,
    val searchDefaultSpanStyle: SpanStyle,
    val searchHighlightSpanStyle: SpanStyle,
    val toolbarColors: ToolbarColors,
    val failureColors: TransciptFailureColor,
) {
    companion object {
        internal val RobotoSerifFontFamily = FontFamily(Font(R.font.roboto_serif))

        fun default(colors: ThemeColors) = TranscriptTheme(
            background = colors.primaryUi01,
            primaryText = colors.primaryText01,
            secondaryText = colors.primaryText02,
            secondaryElement = colors.primaryUi05,
            searchDefaultSpanStyle = SpanStyle(
                background = colors.primaryText01.copy(alpha = 0.6f),
                color = colors.primaryUi01,
            ),
            searchHighlightSpanStyle = SpanStyle(
                background = colors.primaryText01,
                color = colors.primaryUi01,
            ),
            toolbarColors = ToolbarColors.default(colors),
            failureColors = TransciptFailureColor.default(colors),
        )

        fun player(colors: PlayerColors) = TranscriptTheme(
            background = colors.background01,
            primaryText = colors.contrast02,
            secondaryText = colors.contrast04,
            secondaryElement = colors.contrast05,
            searchDefaultSpanStyle = SpanStyle(
                background = Color.White.copy(alpha = 0.2f),
                color = Color.White,
            ),
            searchHighlightSpanStyle = SpanStyle(
                background = Color.White,
                color = Color.Black,
            ),
            toolbarColors = ToolbarColors.player(colors),
            failureColors = TransciptFailureColor.player(colors),
        )
    }
}

@Composable
internal fun rememberTranscriptTheme(): TranscriptTheme {
    val theme = MaterialTheme.theme
    val playerColors = theme.rememberPlayerColors()

    return remember(theme.type, playerColors) {
        if (playerColors != null) {
            TranscriptTheme.player(playerColors)
        } else {
            TranscriptTheme.default(theme.colors)
        }
    }
}
