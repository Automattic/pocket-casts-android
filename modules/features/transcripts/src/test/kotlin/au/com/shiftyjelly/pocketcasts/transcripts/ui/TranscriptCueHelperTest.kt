package au.com.shiftyjelly.pocketcasts.transcripts.ui

import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TranscriptCueHelperTest {

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
