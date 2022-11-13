package au.com.shiftyjelly.pocketcasts.endofyear.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.endofyear.StoriesViewModel.State.Loaded.SegmentsData
import kotlinx.coroutines.flow.StateFlow

private val StrokeWidth = 2.dp
private val SegmentHeight = StrokeWidth
private const val IndicatorBackgroundOpacity = 0.5f

@Composable
fun SegmentedProgressIndicator(
    progressFlow: StateFlow<Float>,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    backgroundColor: Color = color.copy(alpha = IndicatorBackgroundOpacity),
    segmentsData: SegmentsData,
) {
    val progress by progressFlow.collectAsState()
    Canvas(
        modifier
            .progressSemantics(progress)
            .fillMaxWidth()
            .height(SegmentHeight)
            .focusable()
    ) {
        drawSegmentsBackground(backgroundColor, segmentsData)
        drawSegments(progress, color, segmentsData)
    }
}

private fun DrawScope.drawSegmentsBackground(
    color: Color,
    segmentsData: SegmentsData,
) = drawSegments(1f, color, segmentsData)

private fun DrawScope.drawSegments(
    endFraction: Float,
    color: Color,
    segmentsData: SegmentsData,
) {
    val width = size.width
    val height = size.height
    // Start drawing from the vertical center of the stroke
    val yOffset = height / 2

    val barEnd = endFraction * width

    repeat(segmentsData.widths.size) { index ->
        val segmentWidth = segmentsData.widths[index] * width
        val xOffsetStart = segmentsData.xStartOffsets[index] * width

        val shouldDrawLine = xOffsetStart < barEnd
        if (shouldDrawLine) {
            val xOffsetEnd = (xOffsetStart + segmentWidth).coerceAtMost(barEnd)
            // Progress line
            drawLine(color, Offset(xOffsetStart, yOffset), Offset(xOffsetEnd, yOffset), StrokeWidth.toPx())
        }
    }
}
