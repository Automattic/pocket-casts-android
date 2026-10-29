package au.com.shiftyjelly.pocketcasts.views.compose

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em

@Composable
internal fun TextViewLine(
    text: AnnotatedString,
    size: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    isMedium: Boolean = false,
    letterSpacing: TextUnit = 0.em,
    maxLines: Int = 1,
) {
    val density = LocalDensity.current
    val textSizePx = with(density) { size.roundToPx() }
    val lineMetrics = remember(textSizePx, isMedium) {
        Paint().apply {
            textSize = textSizePx.toFloat()
            typeface = if (isMedium) Typeface.create("sans-serif-medium", Typeface.NORMAL) else Typeface.SANS_SERIF
        }.fontMetricsInt
    }
    Text(
        text = text,
        color = color,
        style = TextStyle(
            fontSize = with(density) { textSizePx.toSp() },
            fontFamily = FontFamily.SansSerif,
            fontWeight = if (isMedium) FontWeight.Medium else null,
            letterSpacing = letterSpacing,
            lineBreak = if (maxLines > 1) LineBreak.Paragraph else LineBreak.Unspecified,
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.textViewLineBox(lineMetrics),
    )
}

private fun Modifier.textViewLineBox(metrics: Paint.FontMetricsInt): Modifier {
    return layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val extraLinesHeight = placeable[LastBaseline] - placeable[FirstBaseline]
        layout(placeable.width, metrics.bottom - metrics.top + extraLinesHeight) {
            placeable.place(0, -metrics.top - placeable[FirstBaseline])
        }
    }
}
