package au.com.shiftyjelly.pocketcasts.transcripts.ui

import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptCueHelperTest {

    private fun text(start: Long, end: Long, value: String = "text") = TranscriptEntry.Text(value = value, startTimeMs = start, endTimeMs = end)

    private fun speaker(name: String = "Speaker") = TranscriptEntry.Speaker(name = name)

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
    fun `findCueBinarySearch returns null when refTime falls in a gap between cues`() {
        val entries = listOf(
            text(0, 1000),
            text(5000, 6000),
        )
        // refTime 4500 is in a gap between cues — strict containment returns null
        assertNull(TranscriptCueHelper.findCueBinarySearch(entries, 4500))
    }

    @Test
    fun `findCueBinarySearch returns null when refTime far beyond all entries`() {
        val entries = listOf(
            text(0, 1000),
            text(1001, 2000),
        )
        assertNull(TranscriptCueHelper.findCueBinarySearch(entries, 100_000))
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

    // --- resolveHighlight ---

    @Test
    fun `resolveHighlight shows the cue containing the reference time`() {
        val entries = listOf(
            text(0, 1000),
            text(1001, 2000),
            text(2001, 3000),
        )
        assertEquals(
            HighlightOutcome.Show(entryIndex = 1, wordIndex = null),
            TranscriptCueHelper.resolveHighlight(entries, refTimeMs = 1500, cachedIndex = 0),
        )
    }

    @Test
    fun `resolveHighlight includes the word index when the cue has word timings`() {
        val entries = listOf(
            TranscriptEntry.Text(
                "Hello world.",
                startTimeMs = 0,
                endTimeMs = 2000,
                words = listOf(
                    TranscriptEntry.WordTiming("Hello", 0, 1000, 0, 5),
                    TranscriptEntry.WordTiming("world.", 1000, 2000, 6, 12),
                ),
            ),
        )
        assertEquals(
            HighlightOutcome.Show(entryIndex = 0, wordIndex = 1),
            TranscriptCueHelper.resolveHighlight(entries, refTimeMs = 1500, cachedIndex = 0),
        )
    }

    @Test
    fun `resolveHighlight keeps the previous highlight in a gap between cues`() {
        val entries = listOf(
            text(0, 1000),
            text(5000, 6000),
        )
        // refTime 4500 sits in the gap between the two cues — hold the previous highlight.
        assertEquals(
            HighlightOutcome.Keep,
            TranscriptCueHelper.resolveHighlight(entries, refTimeMs = 4500, cachedIndex = 0),
        )
    }

    @Test
    fun `resolveHighlight keeps the previous highlight after the last cue`() {
        val entries = listOf(
            text(0, 1000),
            text(1001, 2000),
        )
        assertEquals(
            HighlightOutcome.Keep,
            TranscriptCueHelper.resolveHighlight(entries, refTimeMs = 100_000, cachedIndex = 0),
        )
    }

    @Test
    fun `resolveHighlight clears before the first cue`() {
        val entries = listOf(
            text(1000, 2000),
            text(2001, 3000),
        )
        // refTime 500 is before the first cue starts — nothing to highlight yet.
        assertEquals(
            HighlightOutcome.Clear,
            TranscriptCueHelper.resolveHighlight(entries, refTimeMs = 500, cachedIndex = 0),
        )
    }

    // --- isSeekSettled ---

    @Test
    fun `isSeekSettled is true at the seek target`() {
        assertTrue(TranscriptCueHelper.isSeekSettled(posMs = 30_000, seekTargetMs = 30_000))
    }

    @Test
    fun `isSeekSettled is true within tolerance on either side`() {
        assertTrue(TranscriptCueHelper.isSeekSettled(posMs = 31_000, seekTargetMs = 30_000))
        assertTrue(TranscriptCueHelper.isSeekSettled(posMs = 29_000, seekTargetMs = 30_000))
    }

    @Test
    fun `isSeekSettled is false outside tolerance (seek not yet landed)`() {
        // Forward seek: still at the old, earlier position.
        assertFalse(TranscriptCueHelper.isSeekSettled(posMs = 10_000, seekTargetMs = 30_000))
        // Backward seek: still at the old, later position.
        assertFalse(TranscriptCueHelper.isSeekSettled(posMs = 60_000, seekTargetMs = 30_000))
    }

    // --- hasReachedTappedRow ---

    @Test
    fun `hasReachedTappedRow is true when resolved row equals the tapped row`() {
        assertTrue(TranscriptCueHelper.hasReachedTappedRow(HighlightOutcome.Show(entryIndex = 5, wordIndex = null), tappedIndex = 5))
    }

    @Test
    fun `hasReachedTappedRow is true when resolved row is past the tapped row`() {
        assertTrue(TranscriptCueHelper.hasReachedTappedRow(HighlightOutcome.Show(entryIndex = 6, wordIndex = null), tappedIndex = 5))
    }

    @Test
    fun `hasReachedTappedRow is false when resolved row is before the tapped row`() {
        assertFalse(TranscriptCueHelper.hasReachedTappedRow(HighlightOutcome.Show(entryIndex = 4, wordIndex = null), tappedIndex = 5))
    }

    @Test
    fun `hasReachedTappedRow is false for Clear and Keep`() {
        assertFalse(TranscriptCueHelper.hasReachedTappedRow(HighlightOutcome.Clear, tappedIndex = 5))
        assertFalse(TranscriptCueHelper.hasReachedTappedRow(HighlightOutcome.Keep, tappedIndex = 5))
    }
}
