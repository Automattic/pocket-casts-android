package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import android.content.res.Configuration
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.R

object TranscriptDefaults {
    val ContentOffsetTop = 64.dp
    val ContentOffsetBottom = 80.dp
    val TranscriptFontFamily = FontFamily(listOf(Font(R.font.roboto_serif)))
    val SearchOccurrenceDefaultSpanStyle = SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.W500, fontFamily = TranscriptFontFamily, background = Color.White.copy(alpha = .2f), color = Color.White)
    val SearchOccurrenceSelectedSpanStyle = SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.W500, fontFamily = TranscriptFontFamily, background = Color.White, color = Color.Black)

    @Composable
    fun bottomPadding() =
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) 0.dp else 125.dp

    @Composable
    fun scrollToHighlightedTextOffset() =
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) 0.dp else 100.dp

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
