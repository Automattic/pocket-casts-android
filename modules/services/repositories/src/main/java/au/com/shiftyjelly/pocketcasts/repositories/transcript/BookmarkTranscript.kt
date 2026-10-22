package au.com.shiftyjelly.pocketcasts.repositories.transcript

import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import java.text.BreakIterator
import kotlin.math.abs
import kotlin.math.roundToInt

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

    /**
     * The display offset the bookmark glyph marks: where [referenceTimeSecs] lands, kept inside
     * [span] so the glyph never sits outside the passage, or the start of the passage without one.
     */
    fun glyphOffsetIn(span: TextSpan?, referenceTimeSecs: Int?): Int? {
        val offset = referenceTimeSecs?.let { referenceOffsetAt(it * 1000L) } ?: span?.start ?: return null
        if (span == null) return offset
        return offset.coerceIn(span.start, (span.end - 1).coerceAtLeast(span.start))
    }

    /** The display offset a reference time in milliseconds lands on, interpolating between entries. */
    fun referenceOffsetAt(timeMs: Long): Int? {
        val timed = textEntrySpans.filter { it.startTimeMs >= 0 }
        if (timed.isEmpty()) return null
        val index = timed.indexOfLast { it.startTimeMs <= timeMs }.coerceAtLeast(0)
        val entry = timed[index]
        val next = timed.getOrNull(index + 1) ?: return entry.start
        val span = next.startTimeMs - entry.startTimeMs
        if (span <= 0) return entry.start
        val fraction = ((timeMs - entry.startTimeMs).toDouble() / span).coerceIn(0.0, 1.0)
        return entry.start + (fraction * (next.start - entry.start)).roundToInt()
    }

    fun passageDisplaySpan(passage: String, location: Int?): TextSpan? {
        val flat = passageFlatSpan(passage, location) ?: return null
        return TextSpan(flatToDisplayStart[flat.start], flatToDisplayEnd[flat.end - 1])
    }

    fun selectionDisplaySpan(selection: String): TextSpan? {
        val normalized = selection.collapseWhitespace()
        if (normalized.isEmpty()) return null
        val collapsed = StringBuilder()
        val displayOffsets = mutableListOf<Int>()
        var pendingSpace = false
        for (i in displayText.indices) {
            if (displayText[i].isWhitespace()) {
                if (collapsed.isNotEmpty()) pendingSpace = true
            } else {
                if (pendingSpace) {
                    collapsed.append(' ')
                    displayOffsets.add(i)
                    pendingSpace = false
                }
                collapsed.append(displayText[i])
                displayOffsets.add(i)
            }
        }
        val start = collapsed.indexOf(normalized)
        if (start < 0) return null
        return TextSpan(displayOffsets[start], displayOffsets[start + normalized.length - 1] + 1)
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

    fun movePassageStart(passage: TextSpan, index: Int): TextSpan {
        if (displayText.isEmpty()) return passage
        var start = wordBoundaries(index).first.coerceAtMost(passage.end - 1)
        while (start < passage.end - 1 && displayText[start].isWhitespace()) start++
        return TextSpan(start, passage.end)
    }

    fun movePassageEnd(passage: TextSpan, index: Int): TextSpan {
        if (displayText.isEmpty()) return passage
        var end = wordBoundaries(index).second.coerceAtLeast(passage.start + 1)
        while (end > passage.start + 1 && displayText[end - 1].isWhitespace()) end--
        return TextSpan(passage.start, end)
    }

    private fun wordBoundaries(index: Int): Pair<Int, Int> {
        val clamped = index.coerceIn(0, displayText.length - 1)
        val iterator = BreakIterator.getWordInstance().apply { setText(displayText) }
        val start = iterator.preceding(clamped + 1).let { if (it == BreakIterator.DONE) 0 else it }
        val end = iterator.following(clamped).let { if (it == BreakIterator.DONE) displayText.length else it }
        return start to end
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

        fun fromPassage(passage: String) = BookmarkTranscript(
            displayText = passage,
            speakerSpans = emptyList(),
            flatText = "",
            flatToDisplayStart = IntArray(0),
            flatToDisplayEnd = IntArray(0),
            textEntrySpans = emptyList(),
        )

        private val Whitespace = """\s+""".toRegex()

        private fun String.collapseWhitespace() = trim().replace(Whitespace, " ")
    }
}
