package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.animation.core.EaseOut
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import kotlin.math.ceil
import kotlin.math.floor

internal fun wordRanges(text: String, startOffset: Int = 0): List<IntRange> = WordRegex.findAll(text)
    .map { match -> (match.range.first + startOffset)..(match.range.last + startOffset) }
    .toList()

internal fun revealChunks(wordCount: Int): List<IntRange> = buildList {
    var start = 0
    while (start < wordCount) {
        val end = (start + RevealChunkSizes[size % RevealChunkSizes.size]).coerceAtMost(wordCount)
        add(start until end)
        start = end
    }
}

internal fun revealedChunkTarget(chunkCount: Int) = chunkCount + REVEAL_FADE_CHUNKS

internal fun AnnotatedString.revealed(
    words: List<IntRange>,
    chunks: List<IntRange>,
    chunkProgress: Float,
    defaultColor: Color,
): AnnotatedString {
    if (chunks.isEmpty() || chunkProgress >= revealedChunkTarget(chunks.size)) return this
    val visibleChunks = ceil(chunkProgress).toInt().coerceIn(0, chunks.size)
    if (visibleChunks == 0) return AnnotatedString("")
    val end = if (visibleChunks == chunks.size) length else words[chunks[visibleChunks - 1].last].last + 1
    val firstFadingChunk = (floor(chunkProgress - REVEAL_FADE_CHUNKS).toInt() + 1).coerceAtLeast(0)
    val source = this
    return buildAnnotatedString {
        append(source.subSequence(0, end))
        for (chunk in firstFadingChunk until visibleChunks) {
            val alpha = EaseOut.transform(((chunkProgress - chunk) / REVEAL_FADE_CHUNKS).coerceIn(0f, 1f))
            for (word in chunks[chunk]) {
                val range = words[word]
                val color = source.colorAt(range.first) ?: defaultColor
                addStyle(SpanStyle(color = color.copy(alpha = color.alpha * alpha)), range.first, range.last + 1)
            }
        }
    }
}

private fun AnnotatedString.colorAt(offset: Int): Color? = spanStyles
    .lastOrNull { style -> offset >= style.start && offset < style.end && style.item.color.isSpecified }
    ?.item
    ?.color

private val WordRegex = Regex("""[^\s​]+""")
private val RevealChunkSizes = intArrayOf(3, 6, 4, 7, 5, 8, 4, 6)
internal const val REVEAL_FADE_CHUNKS = 4
