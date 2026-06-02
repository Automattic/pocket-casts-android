package au.com.shiftyjelly.pocketcasts.repositories.transcript

import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import com.squareup.moshi.Moshi
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Test

class JsonParserTest {
    private val parser = JsonParser(Moshi.Builder().build())

    @Test
    fun `parse json subtitles`() {
        val subtitles = """
            |{
            |  "segments": [
            |    {
            |      "body": "Text without spekaer"
            |    },
            |    {
            |      "body": "Text with spekaer",
            |      "speaker": "Speaker"
            |    }
            |  ]
            |}
        """.trimMargin()
        val source = Buffer().writeUtf8(subtitles)

        val entries = parser.parse(source).getOrThrow()

        assertEquals(
            listOf(
                TranscriptEntry.Text("Text without spekaer"),
                TranscriptEntry.Speaker("Speaker"),
                TranscriptEntry.Text("Text with spekaer"),
            ),
            entries,
        )
    }

    @Test
    fun `parse json subtitles with timing`() {
        val subtitles = """
            |{
            |  "segments": [
            |    {
            |      "body": "Timed text",
            |      "startTime": 1.5,
            |      "endTime": 3.75
            |    },
            |    {
            |      "body": "No timing"
            |    },
            |    {
            |      "body": "Partial timing",
            |      "startTime": 10.0
            |    }
            |  ]
            |}
        """.trimMargin()
        val source = Buffer().writeUtf8(subtitles)

        val entries = parser.parse(source).getOrThrow()

        assertEquals(
            listOf(
                TranscriptEntry.Text("Timed text", startTimeMs = 1500, endTimeMs = 3750),
                TranscriptEntry.Text("No timing", startTimeMs = -1, endTimeMs = -1),
                TranscriptEntry.Text("Partial timing", startTimeMs = 10000, endTimeMs = -1),
            ),
            entries,
        )
    }
}
