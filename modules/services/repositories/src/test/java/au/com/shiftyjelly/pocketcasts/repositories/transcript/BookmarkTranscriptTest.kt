package au.com.shiftyjelly.pocketcasts.repositories.transcript

import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `passage span is null when the passage is absent`() {
        assertNull(transcript.passageDisplaySpan("a passage no transcript would ever contain", location = null))
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
