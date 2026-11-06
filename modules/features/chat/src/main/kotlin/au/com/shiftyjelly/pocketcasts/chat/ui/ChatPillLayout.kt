package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor

internal class WidestLine {
    private var left = 0
    private var right = Int.MAX_VALUE

    val width get() = right - left
    val offset get() = left

    fun onTextLayout(result: TextLayoutResult) {
        val lines = 0 until result.lineCount
        left = floor(lines.minOf(result::getLineLeft)).toInt()
        right = ceil(lines.maxOf(result::getLineRight)).toInt()
    }
}

internal fun Modifier.hugWidestLine(widestLine: WidestLine) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val width = constraints.constrainWidth(widestLine.width.coerceAtMost(placeable.width))
    layout(width, placeable.height) {
        placeable.place(-widestLine.offset.coerceIn(0, placeable.width - width), 0)
    }
}

internal fun Modifier.chipMaxWidth() = layout { measurable, constraints ->
    val maxWidth = if (constraints.hasBoundedWidth) {
        (constraints.maxWidth - ChipMinStartGap.roundToPx()).coerceAtLeast(constraints.minWidth)
    } else {
        constraints.maxWidth
    }
    val placeable = measurable.measure(constraints.copy(maxWidth = maxWidth))
    layout(placeable.width, placeable.height) {
        placeable.place(0, 0)
    }
}

internal val ChipCornerRadius = 22.dp
private val ChipMinStartGap = 32.dp
