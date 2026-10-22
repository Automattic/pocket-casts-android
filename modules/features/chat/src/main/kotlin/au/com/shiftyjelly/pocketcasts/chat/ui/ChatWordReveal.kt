package au.com.shiftyjelly.pocketcasts.chat.ui

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

internal fun revealedWordTarget(wordCount: Int) = wordCount + REVEAL_FADE_WORDS

internal fun AnnotatedString.revealed(
    words: List<IntRange>,
    wordProgress: Float,
    defaultColor: Color,
): AnnotatedString {
    if (wordProgress >= revealedWordTarget(words.size)) return this
    val visibleWords = ceil(wordProgress).toInt().coerceIn(0, words.size)
    if (visibleWords == 0) return AnnotatedString("")
    val end = if (visibleWords == words.size) length else words[visibleWords - 1].last + 1
    val firstFadingWord = (floor(wordProgress - REVEAL_FADE_WORDS).toInt() + 1).coerceAtLeast(0)
    val source = this
    return buildAnnotatedString {
        append(source.subSequence(0, end))
        for (word in firstFadingWord until visibleWords) {
            val range = words[word]
            val alpha = ((wordProgress - word) / REVEAL_FADE_WORDS).coerceIn(0f, 1f)
            val color = source.colorAt(range.first) ?: defaultColor
            addStyle(SpanStyle(color = color.copy(alpha = color.alpha * alpha)), range.first, range.last + 1)
        }
    }
}

private fun AnnotatedString.colorAt(offset: Int): Color? = spanStyles
    .lastOrNull { style -> offset >= style.start && offset < style.end && style.item.color.isSpecified }
    ?.item
    ?.color

private val WordRegex = Regex("""[^\s​]+""")
internal const val REVEAL_FADE_WORDS = 3
