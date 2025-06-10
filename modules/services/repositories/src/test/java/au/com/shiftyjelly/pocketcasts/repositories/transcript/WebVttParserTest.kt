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
class WebVttParserTest {
    private val parser = WebVttParser()

    @Test
    fun `parse vtt subtitles`() {
        val subtitles = """
            |WEBVTT
            |
            |00:00.000 --> 00:01.000
            |Text
            |
            |00:01.000 --> 00:02.000
            |<v Alice>Text with speaker
            |
            |00:02.000 --> 00:03.000 line:0 position:20% size:60% align:start
            |<b>Text<b/> <00:19.000>with <i>decorations</i>
            |
            |00:03.000 --> 00:04.000
            |<v.first.loud Bob>Text with speaker and with spans
            |
            |00:04.000 --> 00:05.000
            |<v Alice>Text with <v Bob>multiple speakers
        """.trimMargin()
        val source = Buffer().writeUtf8(subtitles)

        val entries = parser.parse(source).getOrThrow()

        assertEquals(
            listOf(
                TranscriptEntry.Text("Text"),
                TranscriptEntry.Speaker("Alice"),
                TranscriptEntry.Text("Text with speaker"),
                TranscriptEntry.Text("Text with decorations"),
                TranscriptEntry.Speaker("Bob"),
                TranscriptEntry.Text("Text with speaker and with spans"),
                TranscriptEntry.Speaker("Alice, Bob"),
                TranscriptEntry.Text("Text with multiple speakers"),
            ),
            entries,
        )
    }
}
