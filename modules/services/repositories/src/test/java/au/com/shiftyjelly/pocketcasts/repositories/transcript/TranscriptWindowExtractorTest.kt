package au.com.shiftyjelly.pocketcasts.repositories.transcript

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Transcript
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.TranscriptService
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import okhttp3.CacheControl
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class TranscriptWindowExtractorTest {

    private val fingerprintTimingManager = mock<FingerprintTimingManager> {
        on { stateFlow } doReturn MutableStateFlow(FingerprintTimingManager.State.Idle)
    }
    private val playbackManager = mock<PlaybackManager>()
    private val currentEpisode = PodcastEpisode(uuid = "episode-id", podcastUuid = "podcast-id", publishedDate = Date())

    private fun extractor(vtt: String) = TranscriptWindowExtractor(
        transcriptDao = mock {
            on { observeTranscripts(any()) } doReturn flowOf(
                listOf(Transcript(episodeUuid = "episode-id", url = "url.vtt", type = "text/vtt", isGenerated = true)),
            )
        },
        transcriptService = object : TranscriptService {
            override suspend fun getTranscriptOrThrow(url: String, cacheControl: CacheControl?) = vtt.toResponseBody()
        },
        fingerprintTimingManager = { fingerprintTimingManager },
        playbackManager = { playbackManager },
    )

    @Test
    fun `map playback time to reference time when mapping is active`() = runTest {
        whenever(fingerprintTimingManager.activeEpisodeUuid).thenReturn("episode-id")
        whenever(fingerprintTimingManager.referenceTime(5000)).thenReturn(25.0)

        val result = extractor(sampleVtt).extractWindow("episode-id", timeSecs = 5)

        assertEquals(windowAroundTwentyFive, result?.passage)
        assertEquals(25, result?.referenceTimeSecs)
        assertEquals(0, result?.location)
    }

    @Test
    fun `return null when mapping is for another episode`() = runTest {
        whenever(fingerprintTimingManager.activeEpisodeUuid).thenReturn("other-episode")

        val result = extractor(sampleVtt).extractWindow("episode-id", timeSecs = 5)

        assertNull(result)
    }

    @Test
    fun `return null when reference time is unavailable and there is no current episode`() = runTest {
        whenever(fingerprintTimingManager.activeEpisodeUuid).thenReturn("episode-id")
        whenever(fingerprintTimingManager.referenceTime(5000)).thenReturn(null)

        val result = extractor(sampleVtt).extractWindow("episode-id", timeSecs = 5)

        assertNull(result)
    }

    @Test
    fun `trigger fingerprinting and wait for the mapping to cover the bookmark`() = runTest {
        val mappingVersion = MutableStateFlow(0L)
        var referenceSecs: Double? = null
        whenever(fingerprintTimingManager.activeEpisodeUuid).thenReturn("episode-id")
        whenever(fingerprintTimingManager.mappingVersion).thenReturn(mappingVersion)
        whenever(fingerprintTimingManager.referenceTime(5000)).thenAnswer { referenceSecs }
        whenever(playbackManager.getCurrentEpisode()).thenReturn(currentEpisode)
        doAnswer {
            referenceSecs = 25.0
            mappingVersion.value++
            null
        }.whenever(fingerprintTimingManager).prepareForCurrentEpisode(FingerprintTimingManager.PrepareTrigger.BOOKMARK)

        val result = extractor(sampleVtt).extractWindow("episode-id", timeSecs = 5)

        assertEquals(windowAroundTwentyFive, result?.passage)
        assertEquals(25, result?.referenceTimeSecs)
    }

    @Test
    fun `wait for the mapping despite a stale unavailable state from another episode`() = runTest {
        val mappingVersion = MutableStateFlow(0L)
        var referenceSecs: Double? = null
        whenever(fingerprintTimingManager.activeEpisodeUuid).thenReturn("episode-id")
        whenever(fingerprintTimingManager.mappingVersion).thenReturn(mappingVersion)
        whenever(fingerprintTimingManager.referenceTime(5000)).thenAnswer { referenceSecs }
        whenever(fingerprintTimingManager.stateFlow).thenReturn(MutableStateFlow(FingerprintTimingManager.State.Unavailable("other-episode")))
        whenever(playbackManager.getCurrentEpisode()).thenReturn(currentEpisode)
        doAnswer {
            referenceSecs = 25.0
            mappingVersion.value++
            null
        }.whenever(fingerprintTimingManager).prepareForCurrentEpisode(FingerprintTimingManager.PrepareTrigger.BOOKMARK)

        val result = extractor(sampleVtt).extractWindow("episode-id", timeSecs = 5)

        assertEquals(windowAroundTwentyFive, result?.passage)
        assertEquals(25, result?.referenceTimeSecs)
    }

    @Test
    fun `return null when the mapping never covers the bookmark`() = runTest {
        whenever(fingerprintTimingManager.activeEpisodeUuid).thenReturn("episode-id")
        whenever(fingerprintTimingManager.referenceTime(5000)).thenReturn(null)
        whenever(fingerprintTimingManager.mappingVersion).thenReturn(MutableStateFlow(0L))
        whenever(playbackManager.getCurrentEpisode()).thenReturn(currentEpisode)

        val result = extractor(sampleVtt).extractWindow("episode-id", timeSecs = 5)

        assertNull(result)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `return null without waiting when fingerprinting is unavailable`() = runTest {
        whenever(fingerprintTimingManager.activeEpisodeUuid).thenReturn("episode-id")
        whenever(fingerprintTimingManager.referenceTime(5000)).thenReturn(null)
        whenever(fingerprintTimingManager.mappingVersion).thenReturn(MutableStateFlow(0L))
        whenever(fingerprintTimingManager.stateFlow).thenReturn(MutableStateFlow(FingerprintTimingManager.State.Unavailable("episode-id")))
        whenever(playbackManager.getCurrentEpisode()).thenReturn(currentEpisode)

        val result = extractor(sampleVtt).extractWindow("episode-id", timeSecs = 5)

        assertEquals(0, currentTime)
        assertNull(result)
    }

    private val sampleVtt = """
        |WEBVTT
        |
        |00:00:00.000 --> 00:00:05.000
        |Welcome to the show everyone.
        |
        |00:00:05.000 --> 00:00:10.000
        |Today we are going to discuss artificial intelligence.
        |
        |00:00:10.000 --> 00:00:20.000
        |Let me start by defining what AI actually means in practice.
        |
        |00:00:20.000 --> 00:00:30.000
        |AI is a broad field that includes machine learning, deep learning, and more.
        |
        |00:00:30.000 --> 00:00:40.000
        |The recent advances have been truly remarkable for the industry.
        |
        |00:00:40.000 --> 00:00:50.000
        |Companies are investing billions of dollars into AI research.
        |
        |00:01:00.000 --> 00:01:10.000
        |This is a much later segment about totally different things.
    """.trimMargin()

    private val windowAroundTwentyFive =
        "Welcome to the show everyone. Today we are going to discuss artificial intelligence. " +
            "Let me start by defining what AI actually means in practice. " +
            "AI is a broad field that includes machine learning, deep learning, and more."

    @Test
    fun `extract window from the cues before and up to the center`() {
        val result = TranscriptWindowExtractor.parseVttWindow(sampleVtt, centerSecs = 30)

        assertEquals(
            "Today we are going to discuss artificial intelligence. " +
                "Let me start by defining what AI actually means in practice. " +
                "AI is a broad field that includes machine learning, deep learning, and more. " +
                "The recent advances have been truly remarkable for the industry.",
            result?.passage,
        )
        assertEquals(30, result?.referenceTimeSecs)
        assertEquals("Welcome to the show everyone.".length + 1, result?.location)
    }

    @Test
    fun `clamp the window start at the beginning of the transcript`() {
        val result = TranscriptWindowExtractor.parseVttWindow(sampleVtt, centerSecs = 5)

        assertEquals(
            "Welcome to the show everyone. Today we are going to discuss artificial intelligence.",
            result?.passage,
        )
        assertEquals(0, result?.location)
    }

    @Test
    fun `return null when window has too few words`() {
        val shortVtt = """
            |WEBVTT
            |
            |00:00:00.000 --> 00:00:05.000
            |Just a few words.
        """.trimMargin()

        val result = TranscriptWindowExtractor.parseVttWindow(shortVtt, centerSecs = 2)

        assertNull(result)
    }

    @Test
    fun `return null when no cues in window`() {
        val result = TranscriptWindowExtractor.parseVttWindow(sampleVtt, centerSecs = 300)

        assertNull(result)
    }

    @Test
    fun `extract window from mm-ss-mmm timestamps`() {
        val vtt = """
            |WEBVTT
            |
            |00:00.000 --> 00:05.000
            |Welcome to the show everyone.
            |
            |00:05.000 --> 00:10.000
            |Today we are going to discuss artificial intelligence.
            |
            |00:10.000 --> 00:20.000
            |Let me start by defining what AI actually means in practice.
            |
            |00:20.000 --> 00:30.000
            |AI is a broad field that includes machine learning, deep learning, and more.
        """.trimMargin()

        val result = TranscriptWindowExtractor.parseVttWindow(vtt, centerSecs = 15)

        assertEquals(
            "Welcome to the show everyone. Today we are going to discuss artificial intelligence. " +
                "Let me start by defining what AI actually means in practice.",
            result?.passage,
        )
    }

    @Test
    fun `strip html tags from cue text`() {
        val vttWithTags = """
            |WEBVTT
            |
            |00:00:00.000 --> 00:00:10.000
            |<v Alice>This is a sentence with enough words to pass the minimum threshold for extraction.
        """.trimMargin()

        val result = TranscriptWindowExtractor.parseVttWindow(vttWithTags, centerSecs = 5)

        assertEquals(
            "This is a sentence with enough words to pass the minimum threshold for extraction.",
            result?.passage,
        )
    }
}
