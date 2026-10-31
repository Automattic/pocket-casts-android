package au.com.shiftyjelly.pocketcasts.compose.text

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

object TextViewAlignment {
    val Center = Alignment { size, space, _ -> IntOffset((space.width - size.width) / 2, (space.height - size.height) / 2) }
    val CenterStart = Alignment { size, space, _ -> IntOffset(0, (space.height - size.height) / 2) }
}

@Composable
fun TextViewText(
    text: String,
    fontSize: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    letterSpacing: TextUnit = 0.em,
    lineSpacingExtra: TextUnit = 0.sp,
    lineSpacingMultiplier: Float = 1f,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE,
    includeFontPadding: Boolean = true,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val textSizePx = with(density) { fontSize.roundToPx() }
    val spacingExtraPx = with(density) { lineSpacingExtra.roundToPx() }
    val metrics = remember(textSizePx, fontWeight) {
        Paint().apply {
            textSize = textSizePx.toFloat()
            typeface = when {
                fontWeight >= FontWeight.Bold -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                fontWeight >= FontWeight.Medium -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
                else -> Typeface.SANS_SERIF
            }
        }.fontMetricsInt
    }
    val style = TextStyle(
        color = color,
        fontSize = textSizePx.toLinearSp(density.density),
        fontFamily = FontFamily.SansSerif,
        fontWeight = fontWeight,
        letterSpacing = letterSpacing,
        textAlign = textAlign,
        lineBreak = LineBreak.Paragraph,
    )
    var lines by remember { mutableStateOf<TextViewLines?>(null) }
    Layout(
        modifier = modifier
            .semantics { this.text = AnnotatedString(text) }
            .drawBehind { lines?.draw(this) },
    ) { _, constraints ->
        val result = textMeasurer.measure(
            text = text,
            style = style,
            overflow = TextOverflow.Ellipsis,
            maxLines = maxLines,
            constraints = Constraints(minWidth = constraints.minWidth, maxWidth = constraints.maxWidth),
            density = Density(density.density),
        )
        val measured = TextViewLines.from(result, metrics, spacingExtraPx, lineSpacingMultiplier, includeFontPadding)
        lines = measured
        layout(
            width = result.size.width,
            height = measured.height.coerceIn(constraints.minHeight, constraints.maxHeight),
            alignmentLines = mapOf(FirstBaseline to measured.baselines.first(), LastBaseline to measured.baselines.last()),
        ) {}
    }
}

private fun Int.toLinearSp(density: Float): TextUnit {
    val sp = this / density
    return (if (sp * density < this) Math.nextUp(sp) else sp).sp
}

private class TextViewLines(
    private val result: TextLayoutResult,
    val baselines: IntArray,
    val height: Int,
) {
    fun draw(scope: DrawScope) {
        for (line in 0 until result.lineCount) {
            val offset = baselines[line] - result.getLineBaseline(line).roundToInt()
            val top = if (line == 0) 0f else result.getLineTop(line) + offset
            val bottom = if (line == result.lineCount - 1) scope.size.height else result.getLineBottom(line) + offset
            scope.clipRect(top = top, bottom = bottom) {
                translate(top = offset.toFloat()) {
                    drawText(result)
                }
            }
        }
    }

    companion object {
        fun from(result: TextLayoutResult, metrics: Paint.FontMetricsInt, spacingExtra: Int, spacingMultiplier: Float, includeFontPadding: Boolean): TextViewLines {
            val lineCount = result.lineCount
            val baselines = IntArray(lineCount)
            val needsSpacing = spacingExtra != 0 || spacingMultiplier != 1f
            var y = 0
            for (line in 0 until lineCount) {
                val isLast = line == lineCount - 1
                val above = if (line == 0 && includeFontPadding) metrics.top else metrics.ascent
                val below = if (isLast && includeFontPadding) metrics.bottom else metrics.descent
                val extra = if (needsSpacing && !isLast) roundExtra((below - above) * (spacingMultiplier - 1.0) + spacingExtra) else 0
                baselines[line] = y - above
                y += below - above + extra
            }
            return TextViewLines(result, baselines, y)
        }

        private fun roundExtra(extra: Double): Int {
            return if (extra >= 0) (extra + 0.5).toInt() else -(-extra + 0.5).toInt()
        }
    }
}
