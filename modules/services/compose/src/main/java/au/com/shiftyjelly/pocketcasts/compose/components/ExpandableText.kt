package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun ExpandableText(
    text: AnnotatedString,
    overflowText: String,
    isExpanded: Boolean,
    style: TextStyle,
    maxLines: Int,
    modifier: Modifier = Modifier,
    onLinkLongPress: ((String) -> Unit)? = null,
    copyLinkAccessibilityLabel: String? = null,
) {
    BoxWithConstraints(
        modifier = modifier.animateContentSize(),
    ) {
        val measurer = rememberTextMeasurer()
        val displayedText = remember(measurer, text, isExpanded) {
            val result = measurer.measure(
                text = text,
                style = style,
                maxLines = maxLines,
                constraints = constraints,
            )
            if (!isExpanded && result.hasVisualOverflow) {
                val lastCharIndex = result.getLineEnd(lineIndex = result.lineCount - 1, visibleEnd = true)
                buildAnnotatedString {
                    // Estimate the number of characters to trim in order to fit the overflow text.
                    // Since character widths vary, we approximate by adding 2 (for the ellipsis and space)
                    // and multiplying by 1.5 to account for potential width differences.
                    // The final trimmed length attempts to give enough of the available space.
                    val trimCharCount = ((overflowText.length + 2) * 1.5f).roundToInt()
                    append(text, 0, (lastCharIndex - trimCharCount).coerceAtLeast(0))
                    append("… ")
                    withStyle(style.toSpanStyle().copy(fontWeight = FontWeight.Bold)) {
                        append(overflowText)
                    }
                }
            } else {
                text
            }
        }
        var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
        Text(
            text = displayedText,
            style = style,
            onTextLayout = { textLayoutResult = it },
            modifier = Modifier.copyLinkOnLongPress(
                text = displayedText,
                textLayoutResult = textLayoutResult,
                accessibilityLabel = copyLinkAccessibilityLabel,
                onLinkLongPress = onLinkLongPress,
            ),
        )
    }
}

private fun Modifier.copyLinkOnLongPress(
    text: AnnotatedString,
    textLayoutResult: TextLayoutResult?,
    accessibilityLabel: String?,
    onLinkLongPress: ((String) -> Unit)?,
): Modifier {
    if (onLinkLongPress == null) return this

    val longPressModifier = pointerInput(text, textLayoutResult, onLinkLongPress) {
        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Initial,
            )
            val url = textLayoutResult?.urlAt(down.position) ?: return@awaitEachGesture
            if (awaitLongPress(down.id, down.position)) {
                onLinkLongPress(url)
                consumeUntilUp()
            }
        }
    }
    val urls = text.urlRanges().map(UrlRange::url).distinct()
    return if (accessibilityLabel == null || urls.isEmpty()) {
        then(longPressModifier)
    } else {
        then(longPressModifier).semantics {
            customActions = urls.map { url ->
                CustomAccessibilityAction(
                    label = "$accessibilityLabel: $url",
                    action = {
                        onLinkLongPress(url)
                        true
                    },
                )
            }
        }
    }
}

private fun TextLayoutResult.urlAt(position: Offset): String? {
    val offset = getOffsetForPosition(position)
    val range = layoutInput.text.urlRangeAt(offset) ?: layoutInput.text.urlRangeAt(offset - 1) ?: return null
    val isInsideLink = (range.start until range.end).any { position in getBoundingBox(it) }
    return range.url.takeIf { isInsideLink }
}

private suspend fun AwaitPointerEventScope.awaitLongPress(
    pointerId: PointerId,
    initialPosition: Offset,
): Boolean {
    val cancelled = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
        while (true) {
            val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == pointerId }
            if (change == null || !change.pressed || (change.position - initialPosition).getDistance() > viewConfiguration.touchSlop) {
                return@withTimeoutOrNull true
            }
        }
    }
    return cancelled == null
}

private suspend fun AwaitPointerEventScope.consumeUntilUp() {
    do {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        event.changes.forEach { it.consume() }
    } while (event.changes.any { it.pressed })
}

internal data class UrlRange(
    val url: String,
    val start: Int,
    val end: Int,
)

internal fun AnnotatedString.urlRangeAt(offset: Int): UrlRange? = urlRanges().firstOrNull { offset in it.start until it.end }

private fun AnnotatedString.urlRanges(): List<UrlRange> = getLinkAnnotations(0, length).mapNotNull { range ->
    val link = range.item as? LinkAnnotation.Url
    if (link == null || link.url.isBlank() || range.start >= range.end) {
        null
    } else {
        UrlRange(
            url = link.url,
            start = range.start,
            end = range.end,
        )
    }
}

@Preview
@Composable
private fun ExpandedTextPreviewRegular() {
    ExpandableTextPreview(
        text = buildAnnotatedString {
            append("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore. ")
            append("Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo. ")
            append("Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla. ")
        },
    )
}

@Preview
@Composable
private fun ExpandedTextPreviewEmptySpace() {
    ExpandableTextPreview(
        text = buildAnnotatedString {
            append("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore. ")
            append("Ut enim ad minim veniam, \n\n")
            append("Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla. ")
        },
    )
}

@Preview
@Composable
private fun ExpandedTextPreviewShort() {
    ExpandableTextPreview(
        text = AnnotatedString("Short text"),
    )
}

@Composable
private fun ExpandableTextPreview(text: AnnotatedString) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .height(200.dp)
            .padding(16.dp),
    ) {
        ExpandableText(
            text = text,
            overflowText = "See more",
            isExpanded = isExpanded,
            style = TextStyle(fontSize = 16.sp, lineHeight = 20.sp),
            maxLines = 4,
            modifier = Modifier
                .background(Color.LightGray)
                .clickable(
                    indication = null,
                    interactionSource = null,
                    onClick = { isExpanded = !isExpanded },
                ),
        )
    }
}
