package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.theme

object TranscriptDefaults {
    val ContentOffsetTop = 64.dp
    val ContentOffsetBottom = 80.dp
    val SearchOccurrenceDefaultSpanStyle = SpanStyle(fontSize = 16.sp, background = Color.DarkGray, color = Color.White)
    val SearchOccurrenceSelectedSpanStyle = SpanStyle(fontSize = 16.sp, background = Color.White, color = Color.Black)

    data class TranscriptColors(
        val playerBackgroundColor: Color,
    ) {
        @Composable
        fun backgroundColor() =
            playerBackgroundColor

        companion object {
            @Composable
            fun contentColor() =
                MaterialTheme.theme.colors.playerContrast06

            @Composable
            fun textColor() =
                MaterialTheme.theme.colors.playerContrast02

            @Composable
            fun iconColor() =
                MaterialTheme.theme.colors.playerContrast02
        }
    }
}
