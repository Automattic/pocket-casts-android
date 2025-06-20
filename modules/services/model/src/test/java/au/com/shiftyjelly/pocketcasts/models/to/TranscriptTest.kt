package au.com.shiftyjelly.pocketcasts.models.to

import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptTest {
    @Test
    fun `excerpt when there are no entries`() {
        val transcript = Transcript.TextPreview.copy(
            entries = emptyList(),
        )

        val excerpt = transcript.getExcerpt()

        assertEquals("", excerpt)
    }

    @Test
    fun `excerpt concatenates entries`() {
        val transcript = Transcript.TextPreview.copy(
            entries = listOf(
                TranscriptEntry.Text("Text 1"),
                TranscriptEntry.Text("Text 2"),
            ),
        )

        val excerpt = transcript.getExcerpt()

        assertEquals("Text 1 Text 2", excerpt)
    }

    @Test
    fun `excerpt ignores speakers entries`() {
        val transcript = Transcript.TextPreview.copy(
            entries = listOf(
                TranscriptEntry.Text("Text 1"),
                TranscriptEntry.Speaker("Text 1"),
                TranscriptEntry.Text("Text 2"),
            ),
        )

        val excerpt = transcript.getExcerpt()

        assertEquals("Text 1 Text 2", excerpt)
    }

    @Test
    fun `excerpt for entries exceeding character limit`() {
        val transcript = Transcript.TextPreview.copy(
            entries = listOf(
                TranscriptEntry.Text("a".repeat(141)),
            ),
        )

        val excerpt = transcript.getExcerpt()

        assertEquals("a".repeat(140) + "…", excerpt)
    }

    @Test
    fun `excerpt for entries matching character limit`() {
        val transcript = Transcript.TextPreview.copy(
            entries = listOf(
                TranscriptEntry.Text("a".repeat(140)),
            ),
        )

        val excerpt = transcript.getExcerpt()

        assertEquals("a".repeat(140), excerpt)
    }

    @Test
    fun `excerpt trims empty space`() {
        val transcript = Transcript.TextPreview.copy(
            entries = listOf(
                TranscriptEntry.Text("a".repeat(120) + " ".repeat(20)),
            ),
        )

        val excerpt = transcript.getExcerpt()

        assertEquals("a".repeat(120), excerpt)
    }
}
