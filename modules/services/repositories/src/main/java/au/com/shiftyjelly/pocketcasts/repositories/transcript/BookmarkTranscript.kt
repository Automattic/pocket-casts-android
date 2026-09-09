package au.com.shiftyjelly.pocketcasts.repositories.transcript

import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import java.text.BreakIterator
import kotlin.math.abs

data class TextSpan(val start: Int, val end: Int) {
    val length get() = end - start
    val isEmpty get() = end <= start
}

class BookmarkTranscript private constructor(
    val displayText: String,
    val speakerSpans: List<TextSpan>,
    private val flatText: String,
    private val flatToDisplayStart: IntArray,
    private val flatToDisplayEnd: IntArray,
    private val textEntrySpans: List<TimedSpan>,
) {
    data class Passage(val text: String, val location: Int)

    private data class TimedSpan(val start: Int, val end: Int, val startTimeMs: Long)

    /** The start time of the transcript entry a display offset lands in, in milliseconds. */
    fun referenceTimeMsAt(displayOffset: Int): Long? {
        val entry = textEntrySpans.firstOrNull { displayOffset in it.start until it.end }
            ?: textEntrySpans.firstOrNull { it.start >= displayOffset }
        return entry?.startTimeMs?.takeIf { it >= 0 }
    }

    fun passageDisplaySpan(passage: String, location: Int?): TextSpan? {
        val flat = passageFlatSpan(passage, location) ?: return null
        return TextSpan(flatToDisplayStart[flat.start], flatToDisplayEnd[flat.end - 1])
    }

    fun sentenceDisplaySpan(index: Int): TextSpan {
        if (displayText.isEmpty()) return TextSpan(0, 0)
        val clamped = index.coerceIn(0, displayText.length - 1)
        val iterator = BreakIterator.getSentenceInstance().apply { setText(displayText) }
        var start = iterator.preceding(clamped + 1).let { if (it == BreakIterator.DONE) 0 else it }
        var end = iterator.following(clamped).let { if (it == BreakIterator.DONE) displayText.length else it }
        while (start < end && displayText[start].isWhitespace()) start++
        while (end > start && displayText[end - 1].isWhitespace()) end--
        return TextSpan(start, end)
    }

    fun passage(displaySpan: TextSpan): Passage {
        if (flatText.isEmpty()) return Passage("", 0)
        var start = 0
        while (start < flatText.length && flatToDisplayEnd[start] <= displaySpan.start) start++
        var end = flatText.length
        while (end > start && flatToDisplayStart[end - 1] >= displaySpan.end) end--
        while (start < end && flatText[start] == ' ') start++
        while (end > start && flatText[end - 1] == ' ') end--
        if (start >= end) return Passage("", 0)
        return Passage(flatText.substring(start, end), start)
    }

    fun displaySubstring(span: TextSpan): String {
        if (span.isEmpty) return ""
        return displayText.substring(span.start.coerceIn(0, displayText.length), span.end.coerceIn(0, displayText.length))
    }

    private fun passageFlatSpan(passage: String, location: Int?): TextSpan? {
        val normalized = passage.collapseWhitespace()
        if (normalized.isEmpty()) return null
        if (location != null && location in 0..flatText.length && flatText.startsWith(normalized, location)) {
            return TextSpan(location, location + normalized.length)
        }
        val start = if (location == null) flatText.indexOf(normalized) else nearestOccurrence(normalized, location)
        if (start < 0) return null
        return TextSpan(start, start + normalized.length)
    }

    private fun nearestOccurrence(value: String, location: Int): Int {
        var nearest = -1
        var nearestDistance = Int.MAX_VALUE
        var index = flatText.indexOf(value)
        while (index >= 0) {
            val distance = abs(index - location)
            if (distance < nearestDistance) {
                nearest = index
                nearestDistance = distance
            }
            index = flatText.indexOf(value, index + 1)
        }
        return nearest
    }

    companion object {
        fun from(transcript: Transcript.Text): BookmarkTranscript {
            val display = StringBuilder()
            val speakerSpans = mutableListOf<TextSpan>()
            val textEntrySpans = mutableListOf<TimedSpan>()
            val flat = StringBuilder()
            val flatStart = mutableListOf<Int>()
            val flatEnd = mutableListOf<Int>()
            var pendingSpace = false

            for (entry in transcript.entries) {
                when (entry) {
                    is TranscriptEntry.Speaker -> {
                        if (display.isNotEmpty()) display.append('\n')
                        val nameStart = display.length
                        display.append(entry.name)
                        speakerSpans.add(TextSpan(nameStart, display.length))
                        if (flat.isNotEmpty()) pendingSpace = true
                    }

                    is TranscriptEntry.Text -> {
                        if (display.isNotEmpty()) display.append('\n')
                        if (flat.isNotEmpty()) pendingSpace = true
                        val valueStart = display.length
                        display.append(entry.value)
                        textEntrySpans.add(TimedSpan(valueStart, display.length, entry.startTimeMs))
                        var i = 0
                        while (i < entry.value.length) {
                            val char = entry.value[i]
                            if (char.isWhitespace()) {
                                if (flat.isNotEmpty()) pendingSpace = true
                                i++
                            } else {
                                if (pendingSpace) {
                                    flat.append(' ')
                                    flatStart.add(valueStart + i)
                                    flatEnd.add(valueStart + i)
                                    pendingSpace = false
                                }
                                flat.append(char)
                                flatStart.add(valueStart + i)
                                flatEnd.add(valueStart + i + 1)
                                i++
                            }
                        }
                    }
                }
            }

            return BookmarkTranscript(
                displayText = display.toString(),
                speakerSpans = speakerSpans,
                flatText = flat.toString(),
                flatToDisplayStart = flatStart.toIntArray(),
                flatToDisplayEnd = flatEnd.toIntArray(),
                textEntrySpans = textEntrySpans,
            )
        }

        private val Whitespace = """\s+""".toRegex()

        private fun String.collapseWhitespace() = trim().replace(Whitespace, " ")
    }
}
