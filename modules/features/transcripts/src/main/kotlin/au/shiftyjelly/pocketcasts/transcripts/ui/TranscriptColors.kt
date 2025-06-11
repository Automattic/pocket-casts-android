package au.shiftyjelly.pocketcasts.transcripts.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import au.com.shiftyjelly.pocketcasts.compose.PlayerColors
import au.com.shiftyjelly.pocketcasts.compose.ThemeColors
import au.com.shiftyjelly.pocketcasts.compose.theme

data class TranscriptColors(
    val background: Color,
    val text: Color,
    val secondaryElement: Color,
) {
    companion object {
        fun default(colors: ThemeColors) = TranscriptColors(
            background = colors.primaryUi01,
            text = colors.primaryText01,
            secondaryElement = colors.primaryUi05,
        )

        fun player(colors: PlayerColors) = TranscriptColors(
            background = colors.background01,
            text = colors.contrast02,
            secondaryElement = colors.contrast05,
        )
    }
}

@Composable
fun rememberTranscriptColors(): TranscriptColors {
    val theme = MaterialTheme.theme
    val playerColors = theme.rememberPlayerColors()

    return remember(theme.type, playerColors) {
        if (playerColors != null) {
            TranscriptColors.player(playerColors)
        } else {
            TranscriptColors.default(theme.colors)
        }
    }
}
