package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.TextUnit

@Composable
internal fun PlaybackText(
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
) {
    Text(
        text = "PLAYBACK",
        color = color,
        fontSize = fontSize,
        fontFamily = humaneFontFamily,
        onTextLayout = onTextLayout,
        modifier = modifier,
    )
}
