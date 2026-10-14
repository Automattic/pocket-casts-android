package au.com.shiftyjelly.pocketcasts.repositories.chat

import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuoteTimingTest {
    private val entries = listOf(
        TranscriptEntry.Speaker("Narrator"),
        TranscriptEntry.Text("The special effects man waits for the verbal cue.", startTimeMs = 1_375_000, endTimeMs = 1_383_000),
        TranscriptEntry.Text(
            value = "A gust travels through the grating, sending the pleated white skirt soaring above her head, revealing her underwear.",
            startTimeMs = 1_383_380,
            endTimeMs = 1_389_840,
            words = listOf(
                word("A gust travels through the grating,", 1_383_380, 1_385_100, 0, 35),
                word("sending the pleated white skirt soaring above her head,", 1_385_600, 1_388_480, 36, 91),
                word("revealing her underwear.", 1_389_100, 1_389_840, 92, 116),
            ),
        ),
        TranscriptEntry.Text("Marilyn acts out the innocent delight of her character.", startTimeMs = 1_391_860, endTimeMs = 1_394_620),
    )

    @Test
    fun `finds quote spanning several cues`() {
        val startMs = entries.findQuoteStartMs(
            quote = "A gust travels through the grating, sending the pleated white skirt soaring above her head, revealing her underwear.",
            hintMs = 82_980_000,
        )

        assertEquals(1_383_380L, startMs)
    }

    @Test
    fun `finds quote starting mid sentence`() {
        assertEquals(1_385_600L, entries.findQuoteStartMs(quote = "“Sending the pleated white skirt soaring”", hintMs = -1))
    }

    @Test
    fun `finds quote spanning several entries`() {
        assertEquals(1_389_100L, entries.findQuoteStartMs(quote = "revealing her underwear. Marilyn acts out", hintMs = -1))
    }

    @Test
    fun `falls back to a unique match of the opening words when the end is reworded`() {
        val startMs = entries.findQuoteStartMs(
            quote = "A gust travels through the grating, sending the pleated skirt up over her head.",
            hintMs = -1,
        )

        assertEquals(1_383_380L, startMs)
    }

    @Test
    fun `ignores opening words that match more than once`() {
        val repeated = listOf(
            TranscriptEntry.Text("This episode is brought to you by our sponsor Acme.", startTimeMs = 60_000, endTimeMs = 62_000),
            TranscriptEntry.Text("This episode is brought to you by our listeners.", startTimeMs = 600_000, endTimeMs = 602_000),
        )

        assertNull(repeated.findQuoteStartMs("This episode is brought to you by our friends at Initech", hintMs = -1))
    }

    @Test
    fun `picks the occurrence closest to the hint`() {
        val repeated = listOf(
            TranscriptEntry.Text("We will be right back after this.", startTimeMs = 60_000, endTimeMs = 62_000),
            TranscriptEntry.Text("We will be right back after this.", startTimeMs = 200_000, endTimeMs = 202_000),
        )

        assertEquals(200_000L, repeated.findQuoteStartMs("We will be right back", hintMs = 190_000))
        assertEquals(60_000L, repeated.findQuoteStartMs("We will be right back", hintMs = 82_980_000))
    }

    @Test
    fun `ignores matches far from the hint`() {
        assertNull(entries.findQuoteStartMs("A gust travels through the grating", hintMs = 1_000))
    }

    @Test
    fun `allows a custom hint distance`() {
        val startMs = entries.findQuoteStartMs("A gust travels through the grating", hintMs = 1_000, maxHintDistanceMs = Long.MAX_VALUE)

        assertEquals(1_383_380L, startMs)
    }

    @Test
    fun `matches whole words only`() {
        val august = listOf(TranscriptEntry.Text("In August travels through the grating", startTimeMs = 0, endTimeMs = 1_000))

        assertNull(august.findQuoteStartMs("gust travels through the grating", hintMs = -1))
    }

    @Test
    fun `ignores short quotes`() {
        assertNull(entries.findQuoteStartMs("revealing her underwear", hintMs = -1))
    }

    @Test
    fun `returns null when the quote is not in the transcript`() {
        assertNull(entries.findQuoteStartMs("She sings happy birthday to the president", hintMs = -1))
    }

    @Test
    fun `returns null when the transcript has no times`() {
        val untimed = listOf(TranscriptEntry.Text("A gust travels through the grating."))

        assertNull(untimed.findQuoteStartMs("A gust travels through the grating", hintMs = -1))
    }

    private fun word(text: String, startMs: Long, endMs: Long, charStart: Int, charEnd: Int) = TranscriptEntry.WordTiming(
        text = text,
        startTimeMs = startMs,
        endTimeMs = endMs,
        charOffsetStart = charStart,
        charOffsetEnd = charEnd,
    )
}
