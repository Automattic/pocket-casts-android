package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import kotlin.math.ceil
import kotlin.math.roundToInt

internal class TextRevealTimeline(text: AnnotatedString) {
    private val length = text.length

    private val pauseOffsets = text.paragraphStyles
        .filter { paragraph -> paragraph.start > 0 && text.text.substring(paragraph.start, paragraph.end).any(Char::isVisible) }
        .map { paragraph -> paragraph.start }
        .distinct()
        .sorted()

    val ticks: Int = length + pauseOffsets.size * PARAGRAPH_PAUSE_TICKS

    fun revealedLength(progress: Float): Float {
        var remaining = progress.coerceAtLeast(0f)
        var position = 0
        for (pauseOffset in pauseOffsets) {
            val run = (pauseOffset - position).toFloat()
            if (remaining <= run) return position + remaining
            remaining -= run + PARAGRAPH_PAUSE_TICKS
            position = pauseOffset
            if (remaining <= 0f) return position.toFloat()
        }
        return (position + remaining).coerceAtMost(length.toFloat())
    }
}

private fun Char.isVisible() = !isWhitespace() && this != ZERO_WIDTH_SPACE.single()

internal class TextWipe {
    var layout: TextLayoutResult? = null

    fun onTextLayout(result: TextLayoutResult) {
        layout = result
    }
}

internal fun Modifier.textWipe(
    wipe: TextWipe,
    revealedLength: () -> Float,
): Modifier = this
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val textLayout = wipe.layout
        val length = revealedLength()
        val height = if (textLayout == null || length >= textLayout.textLength) {
            placeable.height
        } else {
            textLayout.getLineBottom(textLayout.revealingLine(length)).roundToInt().coerceAtMost(placeable.height)
        }
        layout(placeable.width, height) {
            placeable.place(0, 0)
        }
    }
    .drawWithContent {
        val textLayout = wipe.layout
        val length = revealedLength()
        if (textLayout == null || length >= textLayout.textLength) {
            drawContent()
        } else {
            clipPath(textLayout.revealedPath(length, size.width)) {
                this@drawWithContent.drawContent()
            }
        }
    }

private val TextLayoutResult.textLength get() = layoutInput.text.length

private fun TextLayoutResult.revealingOffset(length: Float) = (ceil(length).toInt() - 1).coerceIn(0, textLength - 1)

private fun TextLayoutResult.revealingLine(length: Float) = getLineForOffset(revealingOffset(length))

private fun TextLayoutResult.revealedPath(length: Float, width: Float): Path {
    val offset = revealingOffset(length)
    val line = getLineForOffset(offset)
    val top = getLineTop(line)
    val bottom = getLineBottom(line)
    val box = getBoundingBox(offset)
    val fraction = (length - offset).coerceIn(0f, 1f)
    val isRtl = getParagraphDirection(offset) == ResolvedTextDirection.Rtl
    val edge = if (isRtl) box.right - box.width * fraction else box.left + box.width * fraction
    return Path().apply {
        if (top > 0f) addRect(Rect(0f, 0f, width, top))
        addRect(if (isRtl) Rect(edge, top, width, bottom) else Rect(0f, top, edge, bottom))
    }
}

internal const val PARAGRAPH_PAUSE_TICKS = 15
