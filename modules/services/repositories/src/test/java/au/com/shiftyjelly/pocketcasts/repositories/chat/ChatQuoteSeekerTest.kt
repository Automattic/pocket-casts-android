package au.com.shiftyjelly.pocketcasts.repositories.chat

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptType
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.ChapterSeekResult
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.fingerprint.FingerprintPolicy
import dagger.Lazy
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ChatQuoteSeekerTest {
    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val episode = PodcastEpisode(uuid = EPISODE_UUID, publishedDate = Date())
    private val transcriptManager = mock<TranscriptManager>()
    private val timingManager = mock<FingerprintTimingManager>()

    private val generatedTranscript = transcript(
        isGenerated = true,
        TranscriptEntry.Text("The special effects man waits for the cue.", startTimeMs = 1_375_000, endTimeMs = 1_383_000),
        TranscriptEntry.Text("A gust travels through the grating.", startTimeMs = 1_383_380, endTimeMs = 1_385_100),
    )

    private val authorTranscript = transcript(
        isGenerated = false,
        TranscriptEntry.Text("A gust travels through the grating.", startTimeMs = 1_390_200, endTimeMs = 1_392_000),
    )

    private val generatedQuote = ChatMessage.Quote(
        text = "A gust travels through the grating",
        start = "00:23:03",
        end = "00:23:05",
        startMs = 1_383_000,
        endMs = 1_385_000,
        isFromGeneratedTranscript = true,
    )

    private val authorQuote = generatedQuote.copy(startMs = 1_390_000, isFromGeneratedTranscript = false)

    @Before
    fun setUp() {
        FeatureFlag.setEnabled(Feature.SYNCED_TRANSCRIPTS, true)
    }

    private fun TestScope.seeker(
        appPlatform: AppPlatform = AppPlatform.Phone,
        policy: FingerprintPolicy = FingerprintPolicy.PLATFORM,
    ) = ChatQuoteSeeker(
        transcriptManager = transcriptManager,
        fingerprintTimingManager = Lazy { timingManager },
        appPlatform = appPlatform,
        decodePolicy = mock { on { current() } doReturn policy },
        scope = backgroundScope,
    )

    @Test
    fun `generated quote seeks through the dense fingerprint map`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(EPISODE_UUID)).thenReturn(generatedTranscript)
        whenever(timingManager.densePlaybackTime(EPISODE_UUID, 1_383_380.milliseconds)).thenReturn(1_413_480.milliseconds)

        assertEquals(1_413_480, seeker().seek(generatedQuote))
        verifyBlocking(timingManager, never()) { resolvePlaybackTime(any(), any()) }
    }

    @Test
    fun `generated quote falls back to a bounded resolve`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(EPISODE_UUID)).thenReturn(generatedTranscript)
        whenever(timingManager.resolvePlaybackTime(eq(episode), eq(1_383_380.milliseconds)))
            .thenReturn(ChapterSeekResult.Resolved(1_353_280.milliseconds, usedPrior = false))

        assertEquals(1_353_280, seeker().seek(generatedQuote))
    }

    @Test
    fun `generated quote uses the transcript time when fingerprints cannot resolve it`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(EPISODE_UUID)).thenReturn(generatedTranscript)
        whenever(timingManager.resolvePlaybackTime(any(), any()))
            .thenReturn(ChapterSeekResult.Unresolved(ChapterSeekResult.REASON_NO_REFERENCE))

        assertEquals(1_383_380, seeker().seek(generatedQuote))
    }

    @Test
    fun `generated quote not found in the transcript maps the server time`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(EPISODE_UUID)).thenReturn(generatedTranscript)
        whenever(timingManager.densePlaybackTime(EPISODE_UUID, 1_000.milliseconds)).thenReturn(31_000.milliseconds)
        val quote = generatedQuote.copy(text = "Something that was never said here", startMs = 1_000)

        assertEquals(31_000, seeker().seek(quote))
    }

    @Test
    fun `author quote maps its matching generated text through fingerprints`() = runTest {
        whenever(transcriptManager.loadTranscript(EPISODE_UUID)).thenReturn(authorTranscript)
        whenever(transcriptManager.loadGeneratedTranscript(EPISODE_UUID)).thenReturn(generatedTranscript)
        whenever(timingManager.densePlaybackTime(EPISODE_UUID, 1_383_380.milliseconds)).thenReturn(1_413_480.milliseconds)

        assertEquals(1_413_480, seeker().seek(authorQuote))
    }

    @Test
    fun `author quote uses its own transcript time without a generated match`() = runTest {
        whenever(transcriptManager.loadTranscript(EPISODE_UUID)).thenReturn(authorTranscript)

        assertEquals(1_390_200, seeker().seek(authorQuote))
        verifyBlocking(timingManager, never()) { resolvePlaybackTime(any(), any()) }
    }

    @Test
    fun `author quote ignores a generated transcript returned as the main one`() = runTest {
        whenever(transcriptManager.loadTranscript(EPISODE_UUID)).thenReturn(generatedTranscript)

        assertEquals(1_390_000, seeker().seek(authorQuote.copy(text = "Not in any transcript at all")))
    }

    @Test
    fun `quote keeps the server time without a transcript`() = runTest {
        assertEquals(1_383_000, seeker().seek(generatedQuote))
    }

    @Test
    fun `fingerprints are skipped off phone`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(EPISODE_UUID)).thenReturn(generatedTranscript)

        assertEquals(1_383_380, seeker(appPlatform = AppPlatform.Automotive).seek(generatedQuote))
        verifyBlocking(timingManager, never()) { resolvePlaybackTime(any(), any()) }
    }

    @Test
    fun `fingerprints are skipped when synced transcripts are off`() = runTest {
        FeatureFlag.setEnabled(Feature.SYNCED_TRANSCRIPTS, false)
        whenever(transcriptManager.loadGeneratedTranscript(EPISODE_UUID)).thenReturn(generatedTranscript)

        assertEquals(1_383_380, seeker().seek(generatedQuote))
    }

    @Test
    fun `tap only policy uses the dense map without a bounded resolve`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(EPISODE_UUID)).thenReturn(generatedTranscript)

        assertEquals(1_383_380, seeker(policy = FingerprintPolicy.TAP_ONLY).seek(generatedQuote))
        verifyBlocking(timingManager, never()) { resolvePlaybackTime(any(), any()) }
    }

    @Test
    fun `fingerprint lookups that take too long use the transcript time`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(EPISODE_UUID)).thenReturn(generatedTranscript)
        whenever(timingManager.resolvePlaybackTime(any(), any())).doSuspendableAnswer { awaitCancellation() }

        assertEquals(1_383_380, seeker().seek(generatedQuote))
    }

    @Test
    fun `author quote loads both transcripts at the same time`() = runTest {
        val generatedLoaded = CompletableDeferred<Unit>()
        whenever(transcriptManager.loadTranscript(EPISODE_UUID)).doSuspendableAnswer {
            generatedLoaded.await()
            authorTranscript
        }
        whenever(transcriptManager.loadGeneratedTranscript(EPISODE_UUID)).doSuspendableAnswer {
            generatedLoaded.complete(Unit)
            generatedTranscript
        }

        val quoteTime = seeker().quoteTime(EPISODE_UUID, authorQuote)

        assertEquals(ChatQuoteSeeker.QuoteTime(timeMs = 1_390_200, referenceTimeMs = 1_383_380), quoteTime)
    }

    @Test
    fun `warming up a quote loads its transcript before the tap`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(EPISODE_UUID)).thenReturn(generatedTranscript)

        seeker().warmUpQuote(EPISODE_UUID, generatedQuote)
        runCurrent()

        verifyBlocking(transcriptManager) { loadGeneratedTranscript(EPISODE_UUID) }
    }

    @Test
    fun `a tap while the quote warms up waits for the same transcript load`() = runTest {
        val loaded = CompletableDeferred<Transcript.Text>()
        whenever(transcriptManager.loadGeneratedTranscript(EPISODE_UUID)).doSuspendableAnswer { loaded.await() }
        val seeker = seeker()
        seeker.warmUpQuote(EPISODE_UUID, generatedQuote)
        runCurrent()

        val quoteTime = backgroundScope.async { seeker.quoteTime(EPISODE_UUID, generatedQuote) }
        runCurrent()
        loaded.complete(generatedTranscript)

        assertEquals(ChatQuoteSeeker.QuoteTime(timeMs = 1_383_380, referenceTimeMs = 1_383_380), quoteTime.await())
        verifyBlocking(transcriptManager, times(1)) { loadGeneratedTranscript(EPISODE_UUID) }
    }

    @Test
    fun `a tap after the warm-up finished asks the transcript manager again`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(EPISODE_UUID)).thenReturn(generatedTranscript)
        val seeker = seeker()
        seeker.warmUpQuote(EPISODE_UUID, generatedQuote)
        runCurrent()

        seeker.quoteTime(EPISODE_UUID, generatedQuote)

        verifyBlocking(transcriptManager, times(2)) { loadGeneratedTranscript(EPISODE_UUID) }
    }

    @Test
    fun `quotes of the same episode share one transcript load`() = runTest {
        val loaded = CompletableDeferred<Transcript.Text>()
        whenever(transcriptManager.loadGeneratedTranscript(EPISODE_UUID)).doSuspendableAnswer { loaded.await() }
        val seeker = seeker()

        seeker.warmUpQuote(EPISODE_UUID, generatedQuote)
        seeker.warmUpQuote(EPISODE_UUID, generatedQuote.copy(text = "The special effects man waits", uuid = "other-quote-uuid"))
        runCurrent()
        loaded.complete(generatedTranscript)
        runCurrent()

        verifyBlocking(transcriptManager, times(1)) { loadGeneratedTranscript(EPISODE_UUID) }
    }

    @Test
    fun `a cancelled tap leaves the transcript loading for the next tap`() = runTest {
        val loaded = CompletableDeferred<Transcript.Text>()
        whenever(transcriptManager.loadGeneratedTranscript(EPISODE_UUID)).doSuspendableAnswer { loaded.await() }
        val seeker = seeker()
        val firstTap = backgroundScope.async { seeker.quoteTime(EPISODE_UUID, generatedQuote) }
        runCurrent()
        firstTap.cancel()

        val secondTap = backgroundScope.async { seeker.quoteTime(EPISODE_UUID, generatedQuote) }
        runCurrent()
        loaded.complete(generatedTranscript)

        assertEquals(1_383_380, secondTap.await().timeMs)
        verifyBlocking(transcriptManager, times(1)) { loadGeneratedTranscript(EPISODE_UUID) }
    }

    @Test
    fun `warming up fingerprints prepares them for the chat`() = runTest {
        seeker().warmUpFingerprints(EPISODE_UUID)

        verify(timingManager).onChatQuoteShown(EPISODE_UUID)
    }

    @Test
    fun `warming up fingerprints is skipped off phone`() = runTest {
        seeker(appPlatform = AppPlatform.Automotive).warmUpFingerprints(EPISODE_UUID)

        verify(timingManager, never()).onChatQuoteShown(any())
    }

    @Test
    fun `warming up fingerprints is skipped when the device cannot decode them`() = runTest {
        seeker(policy = FingerprintPolicy.DISABLED).warmUpFingerprints(EPISODE_UUID)

        verify(timingManager, never()).onChatQuoteShown(any())
    }

    @Test
    fun `warming up fingerprints is skipped when synced transcripts are off`() = runTest {
        FeatureFlag.setEnabled(Feature.SYNCED_TRANSCRIPTS, false)

        seeker().warmUpFingerprints(EPISODE_UUID)

        verify(timingManager, never()).onChatQuoteShown(any())
    }

    private suspend fun ChatQuoteSeeker.seek(quote: ChatMessage.Quote) = playbackTimeMs(episode, quoteTime(EPISODE_UUID, quote))

    private fun transcript(isGenerated: Boolean, vararg entries: TranscriptEntry) = Transcript.Text(
        entries = entries.toList(),
        type = TranscriptType.Vtt,
        url = "transcript-url",
        isGenerated = isGenerated,
        episodeUuid = EPISODE_UUID,
        podcastUuid = "podcast-uuid",
    )

    private companion object {
        const val EPISODE_UUID = "episode-uuid"
    }
}
