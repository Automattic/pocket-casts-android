package au.com.shiftyjelly.pocketcasts.appfunctions

import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptFormatterTest {
    private val transcript = Transcript.Text(
        entries = listOf(
            TranscriptEntry.Speaker("Host"),
            TranscriptEntry.Text("Welcome to the show.", startTimeMs = 0),
            TranscriptEntry.Text("Today we talk about bread.", startTimeMs = 65_000),
            TranscriptEntry.Speaker("Guest"),
            TranscriptEntry.Text(" Thanks for having me. ", startTimeMs = 3_725_000),
        ),
        type = TranscriptType.Vtt,
        url = "https://example.com/transcript.vtt",
        isGenerated = true,
        episodeUuid = "episode",
        podcastUuid = "podcast",
    )

    @Test
    fun `formats every line with time and speaker changes`() {
        val result = transcript.toTranscriptResult(fromSeconds = 0, toSeconds = 0, maxChars = 1_000)

        assertEquals(
            """
            [0:00:00] Host: Welcome to the show.
            [0:01:05] Today we talk about bread.
            [1:02:05] Guest: Thanks for having me.
            """.trimIndent(),
            result.text,
        )
        assertTrue(result.isGenerated)
        assertFalse(result.isTruncated)
    }

    @Test
    fun `keeps only lines inside the time window and names the current speaker`() {
        val result = transcript.toTranscriptResult(fromSeconds = 60, toSeconds = 120, maxChars = 1_000)

        assertEquals("[0:01:05] Host: Today we talk about bread.", result.text)
        assertFalse(result.isTruncated)
    }

    @Test
    fun `cuts off at the character limit`() {
        val result = transcript.toTranscriptResult(fromSeconds = 0, toSeconds = 0, maxChars = 50)

        assertEquals("[0:00:00] Host: Welcome to the show.", result.text)
        assertTrue(result.isTruncated)
    }

    @Test
    fun `includes lines without timings`() {
        val untimed = transcript.copy(entries = listOf(TranscriptEntry.Text("No timings here.")))

        val result = untimed.toTranscriptResult(fromSeconds = 0, toSeconds = 0, maxChars = 1_000)

        assertEquals("No timings here.", result.text)
    }
}
