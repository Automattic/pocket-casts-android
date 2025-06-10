package au.shiftyjelly.pocketcasts.transcripts.data

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
        """.trimMargin()
        val source = Buffer().writeUtf8(subtitles)

        val entries = parser.parse(source).getOrThrow()

        assertEquals(
            listOf(
                TranscriptEntry.Text("Text"),
                TranscriptEntry.Text("Text with HTML tags"),
            ),
            entries,
        )
    }
}
