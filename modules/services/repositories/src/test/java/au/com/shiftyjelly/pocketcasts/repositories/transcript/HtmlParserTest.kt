package au.com.shiftyjelly.pocketcasts.repositories.transcript

import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class HtmlParserTest {
    private val parser = HtmlParser()

    @Test
    fun `parse html subtitles`() {
        val subtitles = """
            |<p>
            |    Text
            |</p>
            |<p>
            |    Peter: Text with speaker
            |</p>
            |<p>
            |    <!-- Comment -->
            |    Text with comment
            |</p>
            |<p>
            |    <b>Text</b> <i>with</i> <em>tags</em>
            |</p>
            |<p>
            |    Text with special characters &#34; &#38; &#39; &#60; &#62; &quot; &apos; &amp; &lt; &gt;
            |</p>
        """.trimMargin()
        val source = Buffer().writeUtf8(subtitles)

        val entries = parser.parse(source).getOrThrow()

        assertEquals(
            listOf(
                TranscriptEntry.Text("Text"),
                TranscriptEntry.Speaker("Peter"),
                TranscriptEntry.Text("Text with speaker"),
                TranscriptEntry.Text("Text with comment"),
                TranscriptEntry.Text("Text with tags"),
                TranscriptEntry.Text("Text with special characters \" & ' < > \" ' & < >"),
            ),
            entries,
        )
    }

    @Test
    fun `do not parse html subtitles with script`() {
        val subtitles = """
            |<script></script>
            |<p>
            |    Text
            |</p>
        """.trimMargin()
        val source = Buffer().writeUtf8(subtitles)

        assertThrows(HtmlParser.ScriptDetectedException::class.java) {
            parser.parse(source).getOrThrow()
        }
    }
}
