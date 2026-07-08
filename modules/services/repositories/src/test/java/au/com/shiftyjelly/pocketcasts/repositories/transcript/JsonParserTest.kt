package au.com.shiftyjelly.pocketcasts.repositories.transcript

import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import com.squareup.moshi.Moshi
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
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

    @Test
    fun `parse flightcast json using embedded vtt`() {
        val subtitles = """
            |{
            |  "text": "Hello world from flightcast",
            |  "segments": [
            |    { "start": 0.03, "end": 2.35, "text": "Hello world from flightcast" }
            |  ],
            |  "vtt": "WEBVTT\n\n1\n00:00:00.031 --> 00:00:02.356\nHello world from flightcast\n"
            |}
        """.trimMargin()
        val source = Buffer().writeUtf8(subtitles)

        val entries = parser.parse(source).getOrThrow()

        assertEquals(
            listOf(
                TranscriptEntry.Text("Hello world from flightcast", startTimeMs = 31, endTimeMs = 2356),
            ),
            entries,
        )
    }

    @Test
    fun `parse flightcast json from text segments when no vtt is present`() {
        val subtitles = """
            |{
            |  "segments": [
            |    { "start": 0.0, "end": 1.0, "text": "Only segment text here" }
            |  ]
            |}
        """.trimMargin()
        val source = Buffer().writeUtf8(subtitles)

        val entries = parser.parse(source).getOrThrow()

        assertEquals(
            listOf(
                TranscriptEntry.Text("Only segment text here", startTimeMs = 0, endTimeMs = 1000),
            ),
            entries,
        )
    }

    @Test
    fun `parse podcast index json prefers speaker and body over embedded vtt`() {
        val subtitles = """
            |{
            |  "segments": [
            |    { "body": "Hello there", "speaker": "Alice" }
            |  ],
            |  "vtt": "WEBVTT\n\n1\n00:00:00.000 --> 00:00:01.000\nShould be ignored\n"
            |}
        """.trimMargin()
        val source = Buffer().writeUtf8(subtitles)

        val entries = parser.parse(source).getOrThrow()

        assertEquals(
            listOf(
                TranscriptEntry.Speaker("Alice"),
                TranscriptEntry.Text("Hello there"),
            ),
            entries,
        )
    }
}
