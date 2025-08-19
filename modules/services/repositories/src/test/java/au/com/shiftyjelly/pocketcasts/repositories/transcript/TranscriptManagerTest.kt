package au.com.shiftyjelly.pocketcasts.repositories.transcript

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptType
import au.com.shiftyjelly.pocketcasts.repositories.transcript.HtmlParser.ScriptDetectedException
import au.com.shiftyjelly.pocketcasts.servers.podcast.TranscriptService
import java.util.Date
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import okhttp3.CacheControl
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.BufferedSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import au.com.shiftyjelly.pocketcasts.models.entity.Transcript as DbTranscript

@OptIn(ExperimentalCoroutinesApi::class)
class TranscriptManagerTest {
    private val localTranscriptsFlow = MutableStateFlow(emptyList<DbTranscript>())
    private val service = TestTranscriptService()
    private val parsers = TranscriptType.entries.associateWith { TestParser(it) }

    private val vttDbTranscript = createDbTranscript("text/vtt")
    private val srtDbTranscript = createDbTranscript("application/srt")
    private val subripDbTranscript = createDbTranscript("application/x-subrip")
    private val jsonDbTranscript = createDbTranscript("application/json")
    private val htmlDbTranscript = createDbTranscript("text/html")

    private val transcriptManager = TranscriptManagerImpl(
        transcriptDao = mock {
            on { observeTranscripts(any()) } doReturn localTranscriptsFlow
        },
        transcriptService = service,
        episodeManager = mock {
            onBlocking { findByUuid(any()) } doReturn PodcastEpisode(
                uuid = "episode-id",
                podcastUuid = "podcast-id",
                publishedDate = Date(),
            )
        },
        parsers = parsers,
    )

    @Test
    fun `load vtt transcript`() = runTest {
        localTranscriptsFlow.value = listOf(vttDbTranscript)

        val transcript = transcriptManager.loadTranscript("episode-id")

        assertEquals(
            Transcript.Text(
                entries = TranscriptEntry.PreviewList,
                type = TranscriptType.Vtt,
                url = "transcript-url",
                isGenerated = false,
                episodeUuid = "episode-id",
                podcastUuid = "podcast-id",
            ),
            transcript,
        )
    }

    @Test
    fun `load srt transcript`() = runTest {
        localTranscriptsFlow.value = listOf(srtDbTranscript)

        val transcript = transcriptManager.loadTranscript("episode-id")

        assertEquals(
            Transcript.Text(
                entries = TranscriptEntry.PreviewList,
                type = TranscriptType.Srt,
                url = "transcript-url",
                isGenerated = false,
                episodeUuid = "episode-id",
                podcastUuid = "podcast-id",
            ),
            transcript,
        )
    }

    @Test
    fun `load subrip transcript`() = runTest {
        localTranscriptsFlow.value = listOf(subripDbTranscript)

        val transcript = transcriptManager.loadTranscript("episode-id")

        assertEquals(
            Transcript.Text(
                entries = TranscriptEntry.PreviewList,
                type = TranscriptType.Srt,
                url = "transcript-url",
                isGenerated = false,
                episodeUuid = "episode-id",
                podcastUuid = "podcast-id",
            ),
            transcript,
        )
    }

    @Test
    fun `load json transcript`() = runTest {
        localTranscriptsFlow.value = listOf(jsonDbTranscript)

        val transcript = transcriptManager.loadTranscript("episode-id")

        assertEquals(
            Transcript.Text(
                entries = TranscriptEntry.PreviewList,
                type = TranscriptType.Json,
                url = "transcript-url",
                isGenerated = false,
                episodeUuid = "episode-id",
                podcastUuid = "podcast-id",
            ),
            transcript,
        )
    }

    @Test
    fun `load html transcript`() = runTest {
        localTranscriptsFlow.value = listOf(htmlDbTranscript)

        val transcript = transcriptManager.loadTranscript("episode-id")

        assertEquals(
            Transcript.Text(
                entries = TranscriptEntry.PreviewList,
                type = TranscriptType.Html,
                url = "transcript-url",
                isGenerated = false,
                episodeUuid = "episode-id",
                podcastUuid = "podcast-id",
            ),
            transcript,
        )
    }

    @Test
    fun `load html web-based transcript`() = runTest {
        localTranscriptsFlow.value = listOf(htmlDbTranscript)
        parsers.getValue(TranscriptType.Html).parsingException = ScriptDetectedException()

        val transcript = transcriptManager.loadTranscript("episode-id")

        assertEquals(
            Transcript.Web(
                url = "transcript-url",
                isGenerated = false,
                episodeUuid = "episode-id",
                podcastUuid = "podcast-id",
            ),
            transcript,
        )
    }

    @Test
    fun `prioritize vtt over json transcript`() = runTest {
        localTranscriptsFlow.value = listOf(jsonDbTranscript, vttDbTranscript)

        val transcript = transcriptManager.loadTranscript("episode-id")!!

        assertEquals(TranscriptType.Vtt, transcript.type)
    }

    @Test
    fun `prioritize json over srt transcript`() = runTest {
        localTranscriptsFlow.value = listOf(srtDbTranscript, jsonDbTranscript)

        val transcript = transcriptManager.loadTranscript("episode-id")!!

        assertEquals(TranscriptType.Json, transcript.type)
    }

    @Test
    fun `prioritize srt over html transcript`() = runTest {
        localTranscriptsFlow.value = listOf(htmlDbTranscript, srtDbTranscript)

        val transcript = transcriptManager.loadTranscript("episode-id")!!

        assertEquals(TranscriptType.Srt, transcript.type)
    }

    @Test
    fun `prioritize non-generated over generated transcript`() = runTest {
        localTranscriptsFlow.value = listOf(vttDbTranscript.copy(isGenerated = true), htmlDbTranscript.copy(isGenerated = false))

        val transcript = transcriptManager.loadTranscript("episode-id")!!

        assertFalse(transcript.isGenerated)
    }

    @Test
    fun `do not load unknown transcript format`() = runTest {
        localTranscriptsFlow.value = listOf(createDbTranscript(type = "unknown"))

        val transcript = transcriptManager.loadTranscript("episode-id")

        assertNull(transcript)
    }

    @Test
    fun `do not load transcript if local ones are not loaded in time`() = runTest {
        val transcript = async { transcriptManager.loadTranscript("episode-id") }

        advanceTimeBy(1.minutes)

        assertNull(transcript.await())
    }

    @Test
    fun `load transcript if local ones are loaded in time`() = runTest {
        val transcript = async { transcriptManager.loadTranscript("episode-id") }

        advanceTimeBy(59.seconds)
        localTranscriptsFlow.value = listOf(vttDbTranscript)

        assertNotNull(transcript.await())
    }

    /**
     * This is a quick verification check. Sanitization is verified in [TranscriptSanitizationTest]
     */
    @Test
    fun `sanitize loaded transcript entries`() = runTest {
        localTranscriptsFlow.value = listOf(vttDbTranscript)
        parsers.getValue(TranscriptType.Vtt).entries = listOf(
            TranscriptEntry.Text("\t\n  Empty space padding.\n\t  "),
            TranscriptEntry.Text("Three\n\n\nor\n\n\n\nmore empty lines."),
        )

        val transcript = transcriptManager.loadTranscript("episode-id")

        assertEquals(
            listOf(
                TranscriptEntry.Text("Empty space padding."),
                TranscriptEntry.Text("Three\n\nor\n\nmore empty lines."),
            ),
            (transcript as Transcript.Text).entries,
        )
    }

    @Test
    fun `fail to load transcript if parsing fails`() = runTest {
        localTranscriptsFlow.value = listOf(vttDbTranscript)
        parsers.getValue(TranscriptType.Vtt).parsingException = RuntimeException()

        val transcript = transcriptManager.loadTranscript("episode-id")

        assertNull(transcript)
    }

    @Test
    fun `fail to load transcript if service fails`() = runTest {
        localTranscriptsFlow.value = listOf(vttDbTranscript)
        service.shouldThrow = true

        val transcript = transcriptManager.loadTranscript("episode-id")

        assertNull(transcript)
    }

    @Test
    fun `load next available transcript if parsing fails`() = runTest {
        localTranscriptsFlow.value = listOf(vttDbTranscript, jsonDbTranscript, srtDbTranscript, htmlDbTranscript)
        parsers.getValue(TranscriptType.Vtt).parsingException = RuntimeException()
        parsers.getValue(TranscriptType.Json).parsingException = RuntimeException()
        parsers.getValue(TranscriptType.Srt).parsingException = RuntimeException()

        val transcript = transcriptManager.loadTranscript("episode-id")!!

        assertEquals(TranscriptType.Html, transcript.type)
    }

    @Test
    fun `cache recently parsed transcript`() = runTest {
        localTranscriptsFlow.value = listOf(vttDbTranscript)
        val transcript1 = transcriptManager.loadTranscript("episode-id")

        localTranscriptsFlow.value = emptyList()
        parsers.getValue(TranscriptType.Vtt).parsingException = RuntimeException()
        val transcript2 = transcriptManager.loadTranscript("episode-id")

        assertNotNull(transcript2)
        assertEquals(transcript1, transcript2)
    }

    @Test
    fun `do not use cached transcript for a different episode`() = runTest {
        localTranscriptsFlow.value = listOf(vttDbTranscript)
        transcriptManager.loadTranscript("episode-id")

        parsers.getValue(TranscriptType.Vtt).parsingException = RuntimeException()
        val transcript2 = transcriptManager.loadTranscript("episode-id-2")

        assertNull(transcript2)
    }

    @Test
    fun `blacklist transcripts that fail to parse`() = runTest {
        val parser = parsers.getValue(TranscriptType.Vtt)
        localTranscriptsFlow.value = listOf(vttDbTranscript)

        parser.parsingException = RuntimeException()
        transcriptManager.loadTranscript("episode-id")

        parser.parsingException = null
        val transcript = transcriptManager.loadTranscript("episode-id")

        assertNull(transcript)
    }

    @Test
    fun `blacklist transcripts that fail to be fetched`() = runTest {
        localTranscriptsFlow.value = listOf(vttDbTranscript)

        service.shouldThrow = true
        transcriptManager.loadTranscript("episode-id")

        service.shouldThrow = false
        val transcript = transcriptManager.loadTranscript("episode-id")

        assertNull(transcript)
    }

    @Test
    fun `clear transcript blacklist`() = runTest {
        val parser = parsers.getValue(TranscriptType.Vtt)
        localTranscriptsFlow.value = listOf(vttDbTranscript)

        parser.parsingException = RuntimeException()
        transcriptManager.loadTranscript("episode-id")

        parser.parsingException = null
        transcriptManager.resetInvalidTranscripts("episode-id")
        val transcript = transcriptManager.loadTranscript("episode-id")

        assertNotNull(transcript)
    }
}

private class TestParser(
    override val type: TranscriptType,
) : TranscriptParser {
    var entries = TranscriptEntry.PreviewList
    var parsingException: Exception? = null

    override fun parse(source: BufferedSource): Result<List<TranscriptEntry>> {
        val exception = parsingException
        return if (exception != null) {
            Result.failure(exception)
        } else {
            Result.success(entries)
        }
    }
}

private class TestTranscriptService : TranscriptService {
    var shouldThrow = false

    override suspend fun getTranscriptOrThrow(url: String, cacheControl: CacheControl?): ResponseBody {
        return if (shouldThrow) {
            error("Test exception")
        } else {
            "Ok".toResponseBody()
        }
    }
}

private fun createDbTranscript(
    type: String,
) = DbTranscript(
    episodeUuid = "episode-id",
    url = "transcript-url",
    type = type,
    isGenerated = false,
    language = "en",
)
