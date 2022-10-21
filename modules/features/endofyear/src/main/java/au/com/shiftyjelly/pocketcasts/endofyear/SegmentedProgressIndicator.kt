package au.com.shiftyjelly.pocketcasts.endofyear

import androidx.annotation.FloatRange
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

private val StrokeWidth = 2.dp
private val GapWidth = 8.dp
private val SegmentHeight = StrokeWidth
private const val IndicatorBackgroundOpacity = 0.24f

@Composable
fun SegmentedProgressIndicator(
    @FloatRange(from = 0.0, to = 1.0) progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    backgroundColor: Color = color.copy(alpha = IndicatorBackgroundOpacity),
    numberOfSegments: Int,
) {
    Canvas(
        modifier
            .progressSemantics(progress)
            .fillMaxWidth()
            .height(SegmentHeight)
            .focusable()
    ) {
        drawSegmentsBackground(backgroundColor, numberOfSegments)
        drawSegments(progress, color, numberOfSegments)
    }
}

private fun DrawScope.drawSegmentsBackground(
    color: Color,
    numberOfSegments: Int,
) = drawSegments(1f, color, numberOfSegments)

private fun DrawScope.drawSegments(
    endFraction: Float,
    color: Color,
    numberOfSegments: Int,
) {
    val width = size.width
    val height = size.height
    // Start drawing from the vertical center of the stroke
    val yOffset = height / 2

    val barEnd = endFraction * width

    val segmentWidth = calculateSegmentWidth(numberOfSegments)
    val segmentAndGapWidth = segmentWidth + GapWidth.toPx()

    repeat(numberOfSegments) { index ->
        val xOffsetStart = index * segmentAndGapWidth
        val shouldDrawLine = xOffsetStart < barEnd
        if (shouldDrawLine) {
            val xOffsetEnd = (xOffsetStart + segmentWidth).coerceAtMost(barEnd)
            // Progress line
            drawLine(color, Offset(xOffsetStart, yOffset), Offset(xOffsetEnd, yOffset), StrokeWidth.toPx())
        }
    }
}

private fun DrawScope.calculateSegmentWidth(
    numberOfSegments: Int,
): Float {
    val width = size.width
    val gapsWidth = (numberOfSegments - 1) * GapWidth.toPx()
    return (width - gapsWidth) / numberOfSegments
}
