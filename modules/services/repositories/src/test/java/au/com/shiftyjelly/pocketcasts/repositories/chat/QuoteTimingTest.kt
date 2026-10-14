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
        val timing = entries.findQuoteTiming(
            quote = "A gust travels through the grating, sending the pleated white skirt soaring above her head, revealing her underwear.",
            hintMs = 82_980_000,
        )

        assertEquals(QuoteTiming(startMs = 1_383_380, endMs = 1_389_840), timing)
    }

    @Test
    fun `finds quote starting mid sentence`() {
        val timing = entries.findQuoteTiming(quote = "“Sending the pleated white skirt soaring”", hintMs = -1)

        assertEquals(QuoteTiming(startMs = 1_385_600, endMs = 1_388_480), timing)
    }

    @Test
    fun `finds quote spanning several entries`() {
        val timing = entries.findQuoteTiming(quote = "revealing her underwear. Marilyn acts out", hintMs = -1)

        assertEquals(QuoteTiming(startMs = 1_389_100, endMs = 1_394_620), timing)
    }

    @Test
    fun `falls back to opening words when the end of the quote is reworded`() {
        val timing = entries.findQuoteTiming(
            quote = "A gust travels through the grating, sending the pleated skirt up over her head.",
            hintMs = -1,
        )

        assertEquals(QuoteTiming(startMs = 1_383_380, endMs = 1_388_480), timing)
    }

    @Test
    fun `picks the occurrence closest to the hint`() {
        val repeated = listOf(
            TranscriptEntry.Text("We will be right back after this.", startTimeMs = 60_000, endTimeMs = 62_000),
            TranscriptEntry.Text("We will be right back after this.", startTimeMs = 600_000, endTimeMs = 602_000),
        )

        assertEquals(600_000L, repeated.findQuoteTiming("We will be right back", hintMs = 590_000)?.startMs)
        assertEquals(60_000L, repeated.findQuoteTiming("We will be right back", hintMs = 82_980_000)?.startMs)
    }

    @Test
    fun `matches whole words only`() {
        val august = listOf(TranscriptEntry.Text("In August travels through the grating", startTimeMs = 0, endTimeMs = 1_000))

        assertNull(august.findQuoteTiming("gust travels through the grating", hintMs = -1))
    }

    @Test
    fun `ignores short quotes`() {
        assertNull(entries.findQuoteTiming("revealing her underwear", hintMs = -1))
    }

    @Test
    fun `returns null when the quote is not in the transcript`() {
        assertNull(entries.findQuoteTiming("She sings happy birthday to the president", hintMs = -1))
    }

    @Test
    fun `returns null when the transcript has no times`() {
        val untimed = listOf(TranscriptEntry.Text("A gust travels through the grating."))

        assertNull(untimed.findQuoteTiming("A gust travels through the grating", hintMs = -1))
    }

    private fun word(text: String, startMs: Long, endMs: Long, charStart: Int, charEnd: Int) = TranscriptEntry.WordTiming(
        text = text,
        startTimeMs = startMs,
        endTimeMs = endMs,
        charOffsetStart = charStart,
        charOffsetEnd = charEnd,
    )
}
