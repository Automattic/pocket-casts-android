package au.com.shiftyjelly.pocketcasts.repositories.chat

import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import kotlin.math.abs

internal data class QuoteTiming(
    val startMs: Long,
    val endMs: Long,
)

internal fun List<TranscriptEntry>.findQuoteTiming(quote: String, hintMs: Long): QuoteTiming? {
    val words = quote.normalizedWords()
    if (words.size < QUOTE_MIN_WORDS) return null
    val transcript = NormalizedTranscript(filterIsInstance<TranscriptEntry.Text>().filter { it.startTimeMs >= 0 })
    if (transcript.text.isEmpty()) return null

    val needles = listOfNotNull(
        words.joinToString(" "),
        words.take(QUOTE_PREFIX_WORDS).joinToString(" ").takeIf { words.size > QUOTE_PREFIX_WORDS },
    )
    val lastEndMs = transcript.entries.maxOf { it.endTimeMs }
    val hint = hintMs.takeIf { it in 0..lastEndMs }
    return needles.firstNotNullOfOrNull { needle ->
        val matches = transcript.indexesOf(needle)
            .map { index -> QuoteTiming(transcript.startMsAt(index), transcript.endMsAt(index + needle.length - 1)) }
        if (hint == null) matches.firstOrNull() else matches.minByOrNull { abs(it.startMs - hint) }
    }
}

private class NormalizedTranscript(val entries: List<TranscriptEntry.Text>) {
    private val builder = StringBuilder()
    private val entryIndexes = mutableListOf<Int>()
    private val charOffsets = mutableListOf<Int>()

    init {
        entries.forEachIndexed { entryIndex, entry ->
            var pendingSpace = builder.isNotEmpty()
            entry.value.forEachIndexed { charOffset, char ->
                if (char.isLetterOrDigit()) {
                    if (pendingSpace) {
                        append(' ', entryIndex, charOffset)
                        pendingSpace = false
                    }
                    append(char.lowercaseChar(), entryIndex, charOffset)
                } else if (builder.isNotEmpty()) {
                    pendingSpace = true
                }
            }
        }
    }

    val text = builder.toString()

    fun indexesOf(needle: String): List<Int> {
        val paddedText = " $text "
        val paddedNeedle = " $needle "
        return generateSequence(paddedText.indexOf(paddedNeedle)) { previous ->
            paddedText.indexOf(paddedNeedle, previous + 1)
        }.takeWhile { it >= 0 }.toList()
    }

    private fun append(char: Char, entryIndex: Int, charOffset: Int) {
        builder.append(char)
        entryIndexes += entryIndex
        charOffsets += charOffset
    }

    fun startMsAt(index: Int): Long {
        val entry = entries[entryIndexes[index]]
        return entry.wordAt(charOffsets[index])?.startTimeMs ?: entry.startTimeMs
    }

    fun endMsAt(index: Int): Long {
        val entry = entries[entryIndexes[index]]
        return entry.wordAt(charOffsets[index])?.endTimeMs ?: entry.endTimeMs
    }

    private fun TranscriptEntry.Text.wordAt(charOffset: Int) = words.firstOrNull { word ->
        word.startTimeMs >= 0 && charOffset in word.charOffsetStart..<word.charOffsetEnd
    }
}

private fun String.normalizedWords() = map { if (it.isLetterOrDigit()) it.lowercaseChar() else ' ' }
    .joinToString("")
    .split(' ')
    .filter(String::isNotEmpty)

private const val QUOTE_MIN_WORDS = 4

private const val QUOTE_PREFIX_WORDS = 8
