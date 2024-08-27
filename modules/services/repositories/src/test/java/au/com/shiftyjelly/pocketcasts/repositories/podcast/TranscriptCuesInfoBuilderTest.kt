package au.com.shiftyjelly.pocketcasts.repositories.podcast

import androidx.media3.extractor.text.SubtitleParser
import au.com.shiftyjelly.pocketcasts.models.converter.TranscriptCue
import au.com.shiftyjelly.pocketcasts.models.converter.TranscriptJsonConverter
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.utils.exception.EmptyDataException
import au.com.shiftyjelly.pocketcasts.utils.exception.ParsingException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class TranscriptCuesInfoBuilderTest {
    private val subtitleParserFactory: SubtitleParser.Factory = mock()
    private val transcriptJsonConverter: TranscriptJsonConverter = mock()
    private val transcript: Transcript = Transcript("episode_id", "url", "type")
    private lateinit var transcriptCuesInfoBuilder: TranscriptCuesInfoBuilder

    @Before
    fun setUp() {
        transcriptCuesInfoBuilder = TranscriptCuesInfoBuilder(subtitleParserFactory, transcriptJsonConverter)
    }

    @Test
    fun `html transcript is parsed correctly`() = runTest {
        val response = mock<ResponseBody>()
        whenever(response.string()).thenReturn("<p>Hello World</p>")

        val result = transcriptCuesInfoBuilder.build(transcript.copy(type = TranscriptFormat.HTML.mimeType), response)

        assertEquals("<p>Hello World</p>", result.first().cuesWithTiming.cues.first().text.toString())
    }

    @Test
    fun `json podcast index transcript is parsed correctly`() = runTest {
        val response = mock<ResponseBody>()
        val jsonString = """[{"startTime": 0.0, "endTime": 5.0, "body": "Hello World", "speaker": "John"}]"""
        whenever(response.string()).thenReturn(jsonString)
        whenever(transcriptJsonConverter.fromString(jsonString)).thenReturn(listOf(TranscriptCue(startTime = 0.0, endTime = 5.0, body = "Hello World", speaker = "John")))

        val result = transcriptCuesInfoBuilder.build(transcript.copy(type = TranscriptFormat.JSON_PODCAST_INDEX.mimeType), response)

        assertEquals("Hello World", result.first().cuesWithTiming.cues.first().text.toString())
        assertEquals("John", result.first().cuesAdditionalInfo?.speaker)
    }

    @Test
    fun `unsupported mime type throws exception`() = runTest {
        val response = mock<ResponseBody>()
        val unsupportedTranscript = transcript.copy(type = "unsupported/type")

        assertThrows(UnsupportedOperationException::class.java) {
            transcriptCuesInfoBuilder.build(unsupportedTranscript, response)
        }
    }

    @Test
    fun `empty html transcript throws EmptyDataException`() = runTest {
        val response = mock<ResponseBody>()
        whenever(response.string()).thenReturn("")
        val emptyHtmlTranscript = transcript.copy(type = TranscriptFormat.HTML.mimeType)

        assertThrows(EmptyDataException::class.java) {
            transcriptCuesInfoBuilder.build(emptyHtmlTranscript, response)
        }
    }

    @Test
    fun `empty json podcast index transcript throws EmptyDataException`() = runTest {
        val response = mock<ResponseBody>()
        whenever(response.string()).thenReturn("")
        val emptyJsonTranscript = transcript.copy(type = TranscriptFormat.JSON_PODCAST_INDEX.mimeType)

        assertThrows(EmptyDataException::class.java) {
            transcriptCuesInfoBuilder.build(emptyJsonTranscript, response)
        }
    }

    @Test
    fun `parsing exception is thrown for invalid subtitle data`() = runTest {
        whenever(subtitleParserFactory.supportsFormat(any())).thenReturn(true)
        val response = mock<ResponseBody>()
        whenever(response.bytes()).thenReturn(byteArrayOf(1.toByte()))
        val parser = mock<SubtitleParser>()
        whenever(subtitleParserFactory.create(anyOrNull())).thenReturn(parser)
        whenever(parser.parse(any(), any(), any())).thenThrow(RuntimeException("Parsing error"))

        val invalidSubtitleTranscript = transcript

        assertThrows(ParsingException::class.java) {
            transcriptCuesInfoBuilder.build(invalidSubtitleTranscript, response)
        }
    }
}
