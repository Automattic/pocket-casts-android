package au.com.shiftyjelly.pocketcasts.repositories.transcript

import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class SrtParserTest {
    private val parser = SrtParser()

    @Test
    fun `parse srt subtitles`() {
        val subtitles = """
            |1
            |00:00:00,000 --> 00:00:01,000
            |Text
            |
            |2
            |00:00:01,000 --> 00:00:02,000
            |<i>Text</i> <b>with</b> <font color="white">HTML</font> tags
            |
            |3
            |00:00:02,000 --> 00:00:03,000
            |Speaker 1: Text with speaker
        """.trimMargin()
        val source = Buffer().writeUtf8(subtitles)

        val entries = parser.parse(source).getOrThrow()

        assertEquals(
            listOf(
                TranscriptEntry.Text("Text"),
                TranscriptEntry.Text("Text with HTML tags"),
                TranscriptEntry.Speaker("Speaker 1"),
                TranscriptEntry.Text("Text with speaker"),
            ),
            entries,
        )
    }
}
