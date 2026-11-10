package au.com.shiftyjelly.pocketcasts.repositories.chat

import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import kotlin.math.abs

internal fun List<TranscriptEntry>.findQuoteStartMs(
    quote: String,
    hintMs: Long,
    maxHintDistanceMs: Long = QUOTE_MAX_HINT_DISTANCE_MS,
): Long? {
    val words = quote.normalizedWords()
    if (words.size < QUOTE_MIN_WORDS) return null
    val transcript = NormalizedTranscript(filterIsInstance<TranscriptEntry.Text>().filter { it.startTimeMs >= 0 })
    if (transcript.text.isBlank()) return null

    val lastEndMs = transcript.entries.maxOf { it.endTimeMs }
    val hint = hintMs.takeIf { it in 0..lastEndMs }
    fun matchStarts(needle: String) = transcript.indexesOf(needle)
        .map(transcript::startMsAt)
        .filter { hint == null || abs(it - hint) <= maxHintDistanceMs }

    val fullMatches = matchStarts(words.joinToString(" "))
    if (fullMatches.isNotEmpty()) {
        return if (hint == null) fullMatches.first() else fullMatches.minBy { abs(it - hint) }
    }
    if (words.size <= QUOTE_PREFIX_WORDS) return null
    return matchStarts(words.take(QUOTE_PREFIX_WORDS).joinToString(" ")).singleOrNull()
}

private class NormalizedTranscript(val entries: List<TranscriptEntry.Text>) {
    val text: String
    private val entryIndexes: IntArray
    private val charOffsets: IntArray

    init {
        val builder = StringBuilder()
        val entryIndexList = ArrayList<Int>()
        val charOffsetList = ArrayList<Int>()
        entries.forEachIndexed { entryIndex, entry ->
            var pendingSpace = builder.isNotEmpty()
            entry.value.forEachIndexed { charOffset, char ->
                if (char.isLetterOrDigit()) {
                    if (pendingSpace) {
                        builder.append(' ')
                        entryIndexList += entryIndex
                        charOffsetList += charOffset
                        pendingSpace = false
                    }
                    builder.append(char.lowercaseChar())
                    entryIndexList += entryIndex
                    charOffsetList += charOffset
                } else if (builder.isNotEmpty()) {
                    pendingSpace = true
                }
            }
        }
        text = " $builder "
        entryIndexes = entryIndexList.toIntArray()
        charOffsets = charOffsetList.toIntArray()
    }

    fun indexesOf(needle: String): List<Int> {
        val paddedNeedle = " $needle "
        return generateSequence(text.indexOf(paddedNeedle)) { previous -> text.indexOf(paddedNeedle, previous + 1) }
            .takeWhile { it >= 0 }
            .toList()
    }

    fun startMsAt(matchIndex: Int): Long {
        val entry = entries[entryIndexes[matchIndex]]
        val charOffset = charOffsets[matchIndex]
        val word = entry.words.firstOrNull { it.startTimeMs >= 0 && charOffset in it.charOffsetStart..<it.charOffsetEnd }
        return word?.startTimeMs ?: entry.startTimeMs
    }
}

private fun String.normalizedWords() = map { if (it.isLetterOrDigit()) it.lowercaseChar() else ' ' }
    .joinToString("")
    .split(' ')
    .filter(String::isNotEmpty)

private const val QUOTE_MIN_WORDS = 4

private const val QUOTE_PREFIX_WORDS = 8

internal const val QUOTE_MAX_HINT_DISTANCE_MS = 180_000L
