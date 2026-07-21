package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.models.to.ChapterOrigin
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.Lazy
import java.util.Date
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

class GeneratedChapterSeekerTest {
    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val episode = PodcastEpisode(uuid = "episode-uuid", publishedDate = Date())

    private val generatedChapter = Chapter(
        title = "Generated",
        startTime = 130.seconds,
        endTime = 200.seconds,
        index = 2,
        uiIndex = 3,
        origin = ChapterOrigin.Generated,
        referenceStartTime = 100.seconds,
    )

    private lateinit var timingManager: FingerprintTimingManager

    @Before
    fun setUp() {
        FeatureFlag.setEnabled(Feature.SYNCED_TRANSCRIPTS, true)
        FeatureFlag.setEnabled(Feature.GENERATED_CHAPTERS, true)
        timingManager = mock()
    }

    private fun seeker(appPlatform: AppPlatform = AppPlatform.Phone) = GeneratedChapterSeeker(
        fingerprintTimingManager = Lazy { timingManager },
        appPlatform = appPlatform,
    )

    @Test
    fun `returns null for non-generated chapters`() = runTest {
        val chapter = generatedChapter.copy(origin = ChapterOrigin.ShowNotes)

        assertNull(seeker().resolveSeekTime(episode, chapter))
        verifyBlocking(timingManager, never()) { resolvePlaybackTime(any(), any()) }
    }

    @Test
    fun `returns null when synced transcripts flag is off`() = runTest {
        FeatureFlag.setEnabled(Feature.SYNCED_TRANSCRIPTS, false)

        assertNull(seeker().resolveSeekTime(episode, generatedChapter))
        verifyBlocking(timingManager, never()) { resolvePlaybackTime(any(), any()) }
    }

    @Test
    fun `returns null on non-phone platforms`() = runTest {
        assertNull(seeker(AppPlatform.Automotive).resolveSeekTime(episode, generatedChapter))
        verifyBlocking(timingManager, never()) { resolvePlaybackTime(any(), any()) }
    }

    @Test
    fun `returns null without a reference start time`() = runTest {
        val chapter = generatedChapter.copy(referenceStartTime = null)

        assertNull(seeker().resolveSeekTime(episode, chapter))
        verifyBlocking(timingManager, never()) { resolvePlaybackTime(any(), any()) }
    }

    @Test
    fun `uses the dense mapping fast path without resolving`() = runTest {
        timingManager = mock {
            on { densePlaybackTime(eq("episode-uuid"), eq(100.seconds)) } doReturn 131.5.seconds
        }

        assertEquals(131.5.seconds, seeker().resolveSeekTime(episode, generatedChapter))
        verifyBlocking(timingManager, never()) { resolvePlaybackTime(any(), any()) }
    }

    @Test
    fun `resolves and caches the rounded-up playback time`() = runTest {
        timingManager = mock {
            on { resolvePlaybackTime(any(), eq(100.seconds)) } doReturn
                ChapterSeekResult.Resolved(playbackTime = 130.2.seconds, usedPrior = false)
        }
        val seeker = seeker()

        assertEquals(131.seconds, seeker.resolveSeekTime(episode, generatedChapter))
        assertEquals(131.seconds, seeker.resolveSeekTime(episode, generatedChapter))
        verifyBlocking(timingManager, times(1)) { resolvePlaybackTime(any(), any()) }
    }

    @Test
    fun `returns null and does not cache when unresolved`() = runTest {
        timingManager = mock {
            on { resolvePlaybackTime(any(), any()) } doReturn
                ChapterSeekResult.Unresolved(ChapterSeekResult.REASON_NO_MATCH)
        }
        val seeker = seeker()

        assertNull(seeker.resolveSeekTime(episode, generatedChapter))
        assertNull(seeker.resolveSeekTime(episode, generatedChapter))
        verifyBlocking(timingManager, times(2)) { resolvePlaybackTime(any(), any()) }
    }

    @Test
    fun `cache is dropped when the episode changes`() = runTest {
        timingManager = mock {
            on { resolvePlaybackTime(any(), any()) } doReturn
                ChapterSeekResult.Resolved(playbackTime = 130.0.seconds, usedPrior = true)
        }
        val seeker = seeker()
        val otherEpisode = PodcastEpisode(uuid = "other-uuid", publishedDate = Date())

        seeker.resolveSeekTime(episode, generatedChapter)
        seeker.resolveSeekTime(otherEpisode, generatedChapter)
        seeker.resolveSeekTime(episode, generatedChapter)

        verifyBlocking(timingManager, times(3)) { resolvePlaybackTime(any(), any()) }
    }

    @Test
    fun `cache is dropped when the audio source changes`() = runTest {
        timingManager = mock {
            on { resolvePlaybackTime(any(), any()) } doReturn
                ChapterSeekResult.Resolved(playbackTime = 130.0.seconds, usedPrior = true)
        }
        val seeker = seeker()
        val downloadedEpisode = episode.copy(downloadedFilePath = "/audio.mp3")

        seeker.resolveSeekTime(episode, generatedChapter)
        seeker.resolveSeekTime(downloadedEpisode, generatedChapter)

        verifyBlocking(timingManager, times(2)) { resolvePlaybackTime(any(), any()) }
    }

    @Test
    fun `clears resolving state after a resolve`() = runTest {
        timingManager = mock {
            on { resolvePlaybackTime(any(), any()) } doReturn
                ChapterSeekResult.Resolved(playbackTime = 130.0.seconds, usedPrior = false)
        }
        val seeker = seeker()

        seeker.resolveSeekTime(episode, generatedChapter)

        assertNull(seeker.resolvingChapter.value)
        verify(timingManager).densePlaybackTime("episode-uuid", 100.seconds)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `superseded tap does not clear the new tap's resolving state`() = runTest {
        timingManager = mock {
            on { resolvePlaybackTime(any(), any()) } doSuspendableAnswer { awaitCancellation() }
        }
        val seeker = seeker()
        launch { seeker.resolveSeekTime(episode, generatedChapter) }
        runCurrent()
        val secondTap = launch { seeker.resolveSeekTime(episode, generatedChapter) }
        runCurrent()

        assertEquals(GeneratedChapterSeeker.ResolvingChapter("episode-uuid", 2), seeker.resolvingChapter.value)

        secondTap.cancelAndJoin()

        assertNull(seeker.resolvingChapter.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `cancelActiveResolve cancels the in-flight caller and clears its state`() = runTest {
        timingManager = mock {
            on { resolvePlaybackTime(any(), any()) } doSuspendableAnswer { awaitCancellation() }
        }
        val seeker = seeker()
        val job = launch { seeker.resolveSeekTime(episode, generatedChapter) }
        runCurrent()
        assertEquals(GeneratedChapterSeeker.ResolvingChapter("episode-uuid", 2), seeker.resolvingChapter.value)

        seeker.cancelActiveResolve()
        job.join()

        assertTrue(job.isCancelled)
        assertNull(seeker.resolvingChapter.value)
    }

    @Test
    fun `cancelActiveResolve without an in-flight caller is a no-op`() {
        seeker().cancelActiveResolve()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `resolving chapter index follows the current episode`() = runTest {
        timingManager = mock {
            on { resolvePlaybackTime(any(), any()) } doSuspendableAnswer { awaitCancellation() }
        }
        val seeker = seeker()
        val job = launch { seeker.resolveSeekTime(episode, generatedChapter) }
        runCurrent()

        assertEquals(2, seeker.resolvingChapterIndex(flowOf("episode-uuid")).first())
        assertNull(seeker.resolvingChapterIndex(flowOf("other-uuid")).first())

        job.cancelAndJoin()

        assertNull(seeker.resolvingChapterIndex(flowOf("episode-uuid")).first())
    }
}
