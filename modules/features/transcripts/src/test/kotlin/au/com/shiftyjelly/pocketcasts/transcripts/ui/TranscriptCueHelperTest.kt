package au.com.shiftyjelly.pocketcasts.transcripts.ui

import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TranscriptCueHelperTest {

    private fun text(start: Long, end: Long, value: String = "text") = TranscriptEntry.Text(value = value, startTimeMs = start, endTimeMs = end)

    private fun speaker(name: String = "Speaker") = TranscriptEntry.Speaker(name = name)

    // --- findCueIndex ---

    @Test
    fun `findCueIndex returns null for empty list`() {
        assertNull(TranscriptCueHelper.findCueIndex(emptyList(), 1000, 0))
    }

    @Test
    fun `findCueIndex returns cached index when refTime falls within cached entry`() {
        val entries = listOf(
            text(0, 1000),
            text(1001, 2000),
            text(2001, 3000),
        )
        assertEquals(1, TranscriptCueHelper.findCueIndex(entries, 1500, cachedIndex = 1))
    }

    @Test
    fun `findCueIndex falls through to nearby scan when cache misses`() {
        val entries = listOf(
            text(0, 1000),
            text(1001, 2000),
            text(2001, 3000),
            text(3001, 4000),
        )
        // Cached at 0, but refTime is in entry 2 — within nearby scan range
        assertEquals(2, TranscriptCueHelper.findCueIndex(entries, 2500, cachedIndex = 0))
    }

    @Test
    fun `findCueIndex falls through to binary search when nearby scan misses`() {
        val entries = buildList {
            for (i in 0 until 50) {
                add(text(i * 1000L, (i + 1) * 1000L - 1))
            }
        }
        // Cached at 0, target at index 40 — way beyond scan limit of 10
        assertEquals(40, TranscriptCueHelper.findCueIndex(entries, 40_500, cachedIndex = 0))
    }

    @Test
    fun `findCueIndex coerces cached index to list bounds`() {
        val entries = listOf(text(0, 1000))
        // cachedIndex far beyond list size — should not crash
        assertEquals(0, TranscriptCueHelper.findCueIndex(entries, 500, cachedIndex = 100))
    }

    // --- findCueNearby ---

    @Test
    fun `findCueNearby finds entry scanning forward`() {
        val entries = listOf(
            text(0, 1000),
            text(1001, 2000),
            text(2001, 3000),
        )
        assertEquals(2, TranscriptCueHelper.findCueNearby(entries, 2500, cached = 0, scanLimit = 10))
    }

    @Test
    fun `findCueNearby finds entry scanning backward`() {
        val entries = listOf(
            text(0, 1000),
            text(1001, 2000),
            text(2001, 3000),
        )
        assertEquals(0, TranscriptCueHelper.findCueNearby(entries, 500, cached = 2, scanLimit = 10))
    }

    @Test
    fun `findCueNearby returns null when no match within scan limit`() {
        val entries = buildList {
            for (i in 0 until 30) {
                add(text(i * 1000L, (i + 1) * 1000L - 1))
            }
        }
        // Cached at 0, target at 25 — beyond scan limit of 10
        assertNull(TranscriptCueHelper.findCueNearby(entries, 25_500, cached = 0, scanLimit = 10))
    }

    @Test
    fun `findCueNearby skips Speaker entries`() {
        val entries = listOf(
            text(0, 1000),
            speaker(),
            speaker(),
            text(2001, 3000),
        )
        assertEquals(3, TranscriptCueHelper.findCueNearby(entries, 2500, cached = 0, scanLimit = 10))
    }

    // --- findCueBinarySearch ---

    @Test
    fun `findCueBinarySearch finds entry at exact start time`() {
        val entries = listOf(
            text(0, 999),
            text(1000, 1999),
            text(2000, 2999),
        )
        assertEquals(1, TranscriptCueHelper.findCueBinarySearch(entries, 1000))
    }

    @Test
    fun `findCueBinarySearch finds entry when refTime is mid-range`() {
        val entries = listOf(
            text(0, 999),
            text(1000, 1999),
            text(2000, 2999),
            text(3000, 3999),
            text(4000, 4999),
        )
        assertEquals(3, TranscriptCueHelper.findCueBinarySearch(entries, 3500))
    }

    @Test
    fun `findCueBinarySearch handles interleaved Speaker entries`() {
        val entries = listOf(
            text(0, 999),
            speaker("Alice"),
            text(1000, 1999),
            speaker("Bob"),
            text(2000, 2999),
        )
        assertEquals(4, TranscriptCueHelper.findCueBinarySearch(entries, 2500))
    }

    @Test
    fun `findCueBinarySearch falls back to closest entry when no exact range match`() {
        val entries = listOf(
            text(0, 1000),
            text(5000, 6000),
        )
        // refTime 4500 is in a gap — should find closest entry within threshold
        assertEquals(1, TranscriptCueHelper.findCueBinarySearch(entries, 4500))
    }

    @Test
    fun `findCueBinarySearch returns null when refTime far beyond all entries`() {
        val entries = listOf(
            text(0, 1000),
            text(1001, 2000),
        )
        assertNull(TranscriptCueHelper.findCueBinarySearch(entries, 100_000))
    }

    // --- findClosestTimedEntry ---

    @Test
    fun `findClosestTimedEntry returns closest entry within threshold`() {
        val entries = listOf(
            text(0, 1000),
            text(2000, 3000),
            text(5000, 6000),
        )
        // refTime 4500 is 1500ms from entry[1].end and 500ms from entry[2].start
        assertEquals(2, TranscriptCueHelper.findClosestTimedEntry(entries, 4500, around = 1))
    }

    @Test
    fun `findClosestTimedEntry returns null when all entries beyond threshold`() {
        val entries = listOf(
            text(0, 1000),
        )
        // refTime 20000 is 19000ms away — well beyond 5000ms threshold
        assertNull(TranscriptCueHelper.findClosestTimedEntry(entries, 20_000, around = 0))
    }

    @Test
    fun `findClosestTimedEntry skips Speaker entries`() {
        val entries = listOf(
            speaker(),
            speaker(),
            text(1000, 2000),
            speaker(),
        )
        assertEquals(2, TranscriptCueHelper.findClosestTimedEntry(entries, 1500, around = 1))
    }

    // --- findNearestTimedEntry ---

    @Test
    fun `findNearestTimedEntry finds timed entry to the right`() {
        val entries = listOf(
            speaker(),
            speaker(),
            text(1000, 2000),
        )
        assertEquals(2, TranscriptCueHelper.findNearestTimedEntry(entries, mid = 1, lo = 0, hi = 2))
    }

    @Test
    fun `findNearestTimedEntry finds timed entry to the left`() {
        val entries = listOf(
            text(0, 1000),
            speaker(),
            speaker(),
        )
        assertEquals(0, TranscriptCueHelper.findNearestTimedEntry(entries, mid = 1, lo = 0, hi = 2))
    }

    @Test
    fun `findNearestTimedEntry returns null when no timed entries in range`() {
        val entries = listOf(
            speaker(),
            speaker(),
            speaker(),
        )
        assertNull(TranscriptCueHelper.findNearestTimedEntry(entries, mid = 1, lo = 0, hi = 2))
    }

    // --- findWordIndex ---

    @Test
    fun `findWordIndex returns null for empty words`() {
        val entry = TranscriptEntry.Text("Hello world.", startTimeMs = 0, endTimeMs = 2000)
        assertNull(TranscriptCueHelper.findWordIndex(entry, 500))
    }

    @Test
    fun `findWordIndex finds word within time range`() {
        val entry = TranscriptEntry.Text(
            "Hello world.",
            startTimeMs = 0,
            endTimeMs = 2000,
            words = listOf(
                TranscriptEntry.WordTiming("Hello", 0, 1000, 0, 5),
                TranscriptEntry.WordTiming("world.", 1000, 2000, 6, 12),
            ),
        )
        assertEquals(0, TranscriptCueHelper.findWordIndex(entry, 500))
        assertEquals(1, TranscriptCueHelper.findWordIndex(entry, 1500))
    }

    @Test
    fun `findWordIndex returns last word when past all words`() {
        val entry = TranscriptEntry.Text(
            "Hello world.",
            startTimeMs = 0,
            endTimeMs = 2000,
            words = listOf(
                TranscriptEntry.WordTiming("Hello", 0, 1000, 0, 5),
                TranscriptEntry.WordTiming("world.", 1000, 2000, 6, 12),
            ),
        )
        assertEquals(1, TranscriptCueHelper.findWordIndex(entry, 2500))
    }

    @Test
    fun `findWordIndex returns word at exact start time`() {
        val entry = TranscriptEntry.Text(
            "Hello world.",
            startTimeMs = 0,
            endTimeMs = 2000,
            words = listOf(
                TranscriptEntry.WordTiming("Hello", 0, 1000, 0, 5),
                TranscriptEntry.WordTiming("world.", 1000, 2000, 6, 12),
            ),
        )
        assertEquals(0, TranscriptCueHelper.findWordIndex(entry, 0))
        assertEquals(1, TranscriptCueHelper.findWordIndex(entry, 1000))
    }

    @Test
    fun `findWordIndex skips words without timing`() {
        val entry = TranscriptEntry.Text(
            "Hello world.",
            startTimeMs = 0,
            endTimeMs = 2000,
            words = listOf(
                TranscriptEntry.WordTiming("Hello", -1, -1, 0, 5),
                TranscriptEntry.WordTiming("world.", 1000, 2000, 6, 12),
            ),
        )
        assertEquals(1, TranscriptCueHelper.findWordIndex(entry, 1500))
    }

    @Test
    fun `findWordIndex returns null when before all words`() {
        val entry = TranscriptEntry.Text(
            "Hello world.",
            startTimeMs = 1000,
            endTimeMs = 3000,
            words = listOf(
                TranscriptEntry.WordTiming("Hello", 1000, 2000, 0, 5),
                TranscriptEntry.WordTiming("world.", 2000, 3000, 6, 12),
            ),
        )
        assertNull(TranscriptCueHelper.findWordIndex(entry, 500))
    }

    @Test
    fun `findWordIndex returns last passed word in gap between words`() {
        val entry = TranscriptEntry.Text(
            "Hello world.",
            startTimeMs = 0,
            endTimeMs = 3000,
            words = listOf(
                TranscriptEntry.WordTiming("Hello", 0, 1000, 0, 5),
                TranscriptEntry.WordTiming("world.", 2000, 3000, 6, 12),
            ),
        )
        assertEquals(0, TranscriptCueHelper.findWordIndex(entry, 1500))
    }
}
