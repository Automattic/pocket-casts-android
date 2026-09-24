package au.com.shiftyjelly.pocketcasts.repositories.transcript

import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkTranscriptTest {

    private val firstSentence = "that's the thing about selective admissions."
    private val secondSentence = "The difference between the kid who gets in and the kid who doesn't is often basically noise."
    private val thirdSentence = "Right, and that's why some researchers have floated the lottery idea."

    private val transcript = bookmarkTranscript(
        TranscriptEntry.Speaker("Speaker 1"),
        TranscriptEntry.Text(firstSentence),
        TranscriptEntry.Text(secondSentence),
        TranscriptEntry.Speaker("Speaker 2"),
        TranscriptEntry.Text(thirdSentence),
    )

    @Test
    fun `sentence span expands an index to the sentence around it`() {
        val index = transcript.displayText.indexOf("gets in")

        val span = transcript.sentenceDisplaySpan(index)

        assertEquals(secondSentence, transcript.displaySubstring(span))
    }

    @Test
    fun `moving the passage start snaps to the start of the word`() {
        val passage = transcript.sentenceDisplaySpan(transcript.displayText.indexOf("gets in"))
        val index = transcript.displayText.indexOf("selective") + 3

        val moved = transcript.movePassageStart(passage, index)

        assertEquals("selective admissions.\n$secondSentence", transcript.displaySubstring(moved))
    }

    @Test
    fun `moving the passage start inside the passage trims it`() {
        val passage = transcript.sentenceDisplaySpan(transcript.displayText.indexOf("gets in"))

        val moved = transcript.movePassageStart(passage, transcript.displayText.indexOf("gets in") + 2)

        assertEquals("gets in and the kid who doesn't is often basically noise.", transcript.displaySubstring(moved))
    }

    @Test
    fun `moving the passage start onto a space starts at the next word`() {
        val passage = transcript.sentenceDisplaySpan(transcript.displayText.indexOf("gets in"))

        val moved = transcript.movePassageStart(passage, transcript.displayText.indexOf(" gets in"))

        assertEquals("gets in and the kid who doesn't is often basically noise.", transcript.displaySubstring(moved))
    }

    @Test
    fun `moving the passage end snaps to the end of the word`() {
        val passage = transcript.sentenceDisplaySpan(transcript.displayText.indexOf("gets in"))
        val index = transcript.displayText.indexOf("researchers") + 2

        val moved = transcript.movePassageEnd(passage, index)

        assertEquals("$secondSentence\nSpeaker 2\nRight, and that's why some researchers", transcript.displaySubstring(moved))
        assertEquals("$secondSentence Right, and that's why some researchers", transcript.passage(moved).text)
    }

    @Test
    fun `moving the passage end inside the passage trims it`() {
        val passage = transcript.sentenceDisplaySpan(transcript.displayText.indexOf("gets in"))

        val moved = transcript.movePassageEnd(passage, transcript.displayText.indexOf("gets in"))

        assertEquals("The difference between the kid who gets", transcript.displaySubstring(moved))
    }

    @Test
    fun `moving a passage edge past the other keeps at least one character`() {
        val passage = transcript.sentenceDisplaySpan(transcript.displayText.indexOf("gets in"))

        val start = transcript.movePassageStart(passage, transcript.displayText.length - 1)
        val end = transcript.movePassageEnd(passage, 0)

        assertEquals(TextSpan(passage.end - 1, passage.end), start)
        assertEquals(TextSpan(passage.start, passage.start + 1), end)
    }

    @Test
    fun `sentence span is empty for an empty transcript`() {
        val empty = bookmarkTranscript()

        assertEquals(TextSpan(0, 0), empty.sentenceDisplaySpan(0))
    }

    @Test
    fun `passage text skips speaker names and joins with single spaces`() {
        val passage = "$firstSentence $secondSentence"

        val span = transcript.passageDisplaySpan(passage, location = null)!!

        assertEquals(passage, transcript.passage(span).text)
    }

    @Test
    fun `passage span keeps the transcript line breaks`() {
        val passage = "$firstSentence $secondSentence"

        val span = transcript.passageDisplaySpan(passage, location = null)!!

        assertEquals("$firstSentence\n$secondSentence", transcript.displaySubstring(span))
    }

    @Test
    fun `passage span round trips a captured passage across a speaker change`() {
        val passage = "$secondSentence $thirdSentence"
        val location = flatText().indexOf(passage)

        val span = transcript.passageDisplaySpan(passage, location)!!

        assertEquals(passage, transcript.passage(span).text)
    }

    @Test
    fun `passage span matches a passage stored with its line breaks`() {
        val flattened = "$firstSentence $secondSentence"
        val stored = "$firstSentence\n$secondSentence"

        val span = transcript.passageDisplaySpan(stored, location = null)!!

        assertEquals(flattened, transcript.passage(span).text)
    }

    @Test
    fun `passage span searches for the text when the location does not line up`() {
        val span = transcript.passageDisplaySpan(thirdSentence, location = 0)!!

        assertEquals(thirdSentence, transcript.passage(span).text)
    }

    @Test
    fun `passage span searches for the text without a location`() {
        val span = transcript.passageDisplaySpan(thirdSentence, location = null)!!

        assertEquals(thirdSentence, transcript.passage(span).text)
    }

    @Test
    fun `passage span uses the location to disambiguate a repeated passage`() {
        val repeated = "The lottery idea comes up again and again."
        val duplicated = bookmarkTranscript(
            TranscriptEntry.Text(repeated),
            TranscriptEntry.Text("Some filler in between to keep the two mentions apart."),
            TranscriptEntry.Text(repeated),
        )
        val flat = "The lottery idea comes up again and again. Some filler in between to keep the two mentions apart. The lottery idea comes up again and again."
        val first = flat.indexOf(repeated)
        val second = flat.lastIndexOf(repeated)

        val located = duplicated.passageDisplaySpan(repeated, second)!!
        assertEquals(second, duplicated.passage(located).location)

        val searched = duplicated.passageDisplaySpan(repeated, location = null)!!
        assertEquals(first, duplicated.passage(searched).location)
    }

    @Test
    fun `passage span picks the occurrence nearest a drifted location`() {
        val repeated = "The lottery idea comes up again and again."
        val duplicated = bookmarkTranscript(
            TranscriptEntry.Text(repeated),
            TranscriptEntry.Text("Some filler in between to keep the two mentions apart."),
            TranscriptEntry.Text(repeated),
        )
        val flat = "The lottery idea comes up again and again. Some filler in between to keep the two mentions apart. The lottery idea comes up again and again."
        val second = flat.lastIndexOf(repeated)

        val drifted = duplicated.passageDisplaySpan(repeated, second + 2)!!

        assertEquals(second, duplicated.passage(drifted).location)
    }

    @Test
    fun `passage span is null when the passage is absent`() {
        assertNull(transcript.passageDisplaySpan("a passage no transcript would ever contain", location = null))
    }

    @Test
    fun `selection span skips a speaker name inside the selection`() {
        val selection = "$secondSentence\nSpeaker 2\n$thirdSentence"

        val span = transcript.selectionDisplaySpan(selection)!!

        assertEquals("$secondSentence $thirdSentence", transcript.passage(span).text)
    }

    @Test
    fun `selection span ignores a leading speaker name`() {
        val selection = "Speaker 2\n$thirdSentence"

        val span = transcript.selectionDisplaySpan(selection)!!

        assertEquals(thirdSentence, transcript.passage(span).text)
    }

    @Test
    fun `selection span across a speaker change is not found by the flat passage matcher`() {
        val selection = "$secondSentence\nSpeaker 2\n$thirdSentence"

        assertNull(transcript.passageDisplaySpan(selection, location = null))
    }

    @Test
    fun `selection span is null for absent text`() {
        assertNull(transcript.selectionDisplaySpan("nothing like this appears in the transcript"))
    }

    @Test
    fun `reference time maps a display offset to its entry start time`() {
        val timed = bookmarkTranscript(
            TranscriptEntry.Text("First line.", startTimeMs = 1000),
            TranscriptEntry.Text("Second line.", startTimeMs = 5000),
        )

        assertEquals(1000L, timed.referenceTimeMsAt(0))
        assertEquals(5000L, timed.referenceTimeMsAt(timed.displayText.indexOf("Second")))
    }

    @Test
    fun `reference time is null for an untimed entry`() {
        val untimed = bookmarkTranscript(TranscriptEntry.Text("No timing here."))

        assertNull(untimed.referenceTimeMsAt(0))
    }

    @Test
    fun `reference offset interpolates a time between entry start times`() {
        val timed = bookmarkTranscript(
            TranscriptEntry.Text("First line.", startTimeMs = 1000),
            TranscriptEntry.Text("Second line.", startTimeMs = 5000),
        )
        val secondStart = timed.displayText.indexOf("Second")

        assertEquals(0, timed.referenceOffsetAt(1000))
        assertEquals(secondStart, timed.referenceOffsetAt(5000))
        assertTrue(timed.referenceOffsetAt(3000)!! in 1 until secondStart)
    }

    @Test
    fun `reference offset is null for an untimed transcript`() {
        val untimed = bookmarkTranscript(TranscriptEntry.Text("No timing here."))

        assertNull(untimed.referenceOffsetAt(1000))
    }

    @Test
    fun `passage-only transcript exposes the passage as its display text without timing`() {
        val passage = "A captured line with no surrounding transcript."

        val model = BookmarkTranscript.fromPassage(passage)

        assertEquals(passage, model.displayText)
        assertNull(model.referenceOffsetAt(0))
    }

    private fun flatText() = listOf(firstSentence, secondSentence, thirdSentence).joinToString(" ")

    private fun bookmarkTranscript(vararg entries: TranscriptEntry) = BookmarkTranscript.from(
        Transcript.Text(
            entries = entries.toList(),
            type = TranscriptType.Vtt,
            url = "https://example.com/transcript.vtt",
            isGenerated = true,
            episodeUuid = "episode-uuid",
            podcastUuid = "podcast-uuid",
        ),
    )
}
