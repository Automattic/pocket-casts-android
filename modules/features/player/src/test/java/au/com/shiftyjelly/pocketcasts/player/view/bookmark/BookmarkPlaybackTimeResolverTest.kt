package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.ChapterSeekResult
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.util.Date
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

class BookmarkPlaybackTimeResolverTest {

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val fingerprintTimingManager = mock<FingerprintTimingManager>()
    private val episode = PodcastEpisode(uuid = "episode", publishedDate = Date())
    private val resolver = BookmarkPlaybackTimeResolver(fingerprintTimingManager)

    @Test
    fun `resolves the reference time to a playback position`() = runTest {
        FeatureFlag.setEnabled(Feature.SYNCED_TRANSCRIPTS, true)
        whenever(fingerprintTimingManager.resolvePlaybackTime(any(), any()))
            .thenReturn(ChapterSeekResult.Resolved(42.seconds, usedPrior = false))

        val result = resolver.playbackTimeMs(episode, referenceTimeSecs = 25, fallbackTimeSecs = 10)

        assertEquals(42_000, result)
    }

    @Test
    fun `falls back to the stored time when the reference time cannot be resolved`() = runTest {
        FeatureFlag.setEnabled(Feature.SYNCED_TRANSCRIPTS, true)
        whenever(fingerprintTimingManager.resolvePlaybackTime(any(), any()))
            .thenReturn(ChapterSeekResult.Unresolved(ChapterSeekResult.REASON_NO_MATCH))

        val result = resolver.playbackTimeMs(episode, referenceTimeSecs = 25, fallbackTimeSecs = 10)

        assertEquals(10_000, result)
    }

    @Test
    fun `falls back to the stored time when there is no reference time`() = runTest {
        FeatureFlag.setEnabled(Feature.SYNCED_TRANSCRIPTS, true)

        val result = resolver.playbackTimeMs(episode, referenceTimeSecs = null, fallbackTimeSecs = 10)

        assertEquals(10_000, result)
        verifyBlocking(fingerprintTimingManager, never()) { resolvePlaybackTime(any(), any()) }
    }

    @Test
    fun `falls back to the stored time when synced transcripts are disabled`() = runTest {
        FeatureFlag.setEnabled(Feature.SYNCED_TRANSCRIPTS, false)

        val result = resolver.playbackTimeMs(episode, referenceTimeSecs = 25, fallbackTimeSecs = 10)

        assertEquals(10_000, result)
        verifyBlocking(fingerprintTimingManager, never()) { resolvePlaybackTime(any(), any()) }
    }
}
