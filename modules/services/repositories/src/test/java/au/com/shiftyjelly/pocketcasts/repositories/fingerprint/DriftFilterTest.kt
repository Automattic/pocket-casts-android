package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import android.content.Context
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager.TimeMappingEntry
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.ChapterManager
import com.automattic.eventhorizon.EventHorizon
import dagger.Lazy
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class DriftFilterTest {

    private lateinit var manager: FingerprintTimingManager

    @Before
    fun setUp() {
        val playbackManager = mock(PlaybackManager::class.java)
        whenever(playbackManager.playbackStateFlow).thenReturn(MutableStateFlow(PlaybackState()))
        manager = FingerprintTimingManager(
            playbackManager = playbackManager,
            referenceRetriever = mock(FingerprintReferenceRetriever::class.java),
            eventHorizon = mock(EventHorizon::class.java),
            context = mock(Context::class.java),
            chapterManager = Lazy { mock(ChapterManager::class.java) },
        )
        manager.debugTrackingEnabled = true
    }

    @Test
    fun `insert maintains sorted order by playback time`() {
        manager.insert(TimeMappingEntry(playbackTime = 10.0, referenceTime = 10.0))
        manager.insert(TimeMappingEntry(playbackTime = 5.0, referenceTime = 5.0))
        manager.insert(TimeMappingEntry(playbackTime = 15.0, referenceTime = 15.0))

        assertEquals(10.0, manager.referenceTime(forPlaybackTimeMs = 10_000)!!, 0.001)
        assertEquals(5.0, manager.referenceTime(forPlaybackTimeMs = 5_000)!!, 0.001)
        assertEquals(15.0, manager.referenceTime(forPlaybackTimeMs = 15_000)!!, 0.001)
    }

    @Test
    fun `insert maintains sorted order by reference time for reverse lookup`() {
        manager.insert(TimeMappingEntry(playbackTime = 10.0, referenceTime = 20.0))
        manager.insert(TimeMappingEntry(playbackTime = 5.0, referenceTime = 10.0))
        manager.insert(TimeMappingEntry(playbackTime = 15.0, referenceTime = 30.0))

        assertEquals(10_000, manager.playbackTimeMs(forReferenceTime = 20.0)!!)
        assertEquals(5_000, manager.playbackTimeMs(forReferenceTime = 10.0)!!)
        assertEquals(15_000, manager.playbackTimeMs(forReferenceTime = 30.0)!!)
    }

    @Test
    fun `referenceTime returns null with no mappings`() {
        assertNull(manager.referenceTime(forPlaybackTimeMs = 5_000))
    }

    @Test
    fun `playbackTimeMs returns null with no mappings`() {
        assertNull(manager.playbackTimeMs(forReferenceTime = 5.0))
    }

    @Test
    fun `consider accepts consistent sequence after bootstrap count`() {
        val entries = listOf(
            TimeMappingEntry(playbackTime = 10.0, referenceTime = 10.0, score = 0.9f),
            TimeMappingEntry(playbackTime = 12.0, referenceTime = 12.0, score = 0.9f),
            TimeMappingEntry(playbackTime = 14.0, referenceTime = 14.0, score = 0.9f),
        )
        manager.stubMatches(entries)

        assertNotNull(manager.referenceTime(forPlaybackTimeMs = 10_000))
        assertNotNull(manager.referenceTime(forPlaybackTimeMs = 12_000))
        assertNotNull(manager.referenceTime(forPlaybackTimeMs = 14_000))
    }

    @Test
    fun `consider rejects inconsistent candidates`() {
        val inserted1 = manager.consider(
            TimeMappingEntry(playbackTime = 10.0, referenceTime = 10.0, score = 0.9f),
        )
        assertEquals(0, inserted1)

        val inserted2 = manager.consider(
            TimeMappingEntry(playbackTime = 12.0, referenceTime = 100.0, score = 0.9f),
        )
        assertEquals(0, inserted2)

        val inserted3 = manager.consider(
            TimeMappingEntry(playbackTime = 14.0, referenceTime = 102.0, score = 0.9f),
        )
        assertEquals(0, inserted3)
    }

    @Test
    fun `consider fast-paths entries consistent with trusted anchor`() {
        val bootstrap = listOf(
            TimeMappingEntry(playbackTime = 10.0, referenceTime = 10.0, score = 0.9f),
            TimeMappingEntry(playbackTime = 12.0, referenceTime = 12.0, score = 0.9f),
            TimeMappingEntry(playbackTime = 14.0, referenceTime = 14.0, score = 0.9f),
        )
        manager.stubMatches(bootstrap)

        val inserted = manager.consider(
            TimeMappingEntry(playbackTime = 16.0, referenceTime = 16.0, score = 0.9f),
        )
        assertEquals(1, inserted)
    }

    @Test
    fun `consider records rejections on eviction`() {
        // Feed 3 inconsistent candidates — first one gets evicted when pool exceeds bootstrap count
        manager.consider(TimeMappingEntry(playbackTime = 10.0, referenceTime = 10.0, score = 0.8f))
        manager.consider(TimeMappingEntry(playbackTime = 12.0, referenceTime = 100.0, score = 0.8f))
        manager.consider(TimeMappingEntry(playbackTime = 14.0, referenceTime = 200.0, score = 0.8f))

        val rejections = manager.debugRejectionsSnapshot
        assertTrue("Expected at least 1 rejection from eviction", rejections.isNotEmpty())
        assertEquals(10.0, rejections.first().playbackTime, 0.001)
    }

    @Test
    fun `consider records rejections on pool flush`() {
        // Bootstrap a trusted anchor
        val bootstrap = listOf(
            TimeMappingEntry(playbackTime = 10.0, referenceTime = 10.0, score = 0.9f),
            TimeMappingEntry(playbackTime = 12.0, referenceTime = 12.0, score = 0.9f),
            TimeMappingEntry(playbackTime = 14.0, referenceTime = 14.0, score = 0.9f),
        )
        manager.stubMatches(bootstrap)

        // Add inconsistent candidate to pool
        manager.consider(TimeMappingEntry(playbackTime = 16.0, referenceTime = 200.0, score = 0.8f))

        // Then add an in-trend candidate — should flush pool and record the inconsistent one as rejection
        manager.consider(TimeMappingEntry(playbackTime = 18.0, referenceTime = 18.0, score = 0.9f))

        val rejections = manager.debugRejectionsSnapshot
        assertTrue("Expected rejection from pool flush", rejections.any { it.playbackTime == 16.0 })
    }

    @Test
    fun `consider records pre-bootstrap candidates as rejections on bootstrap acceptance`() {
        // Feed 2 noise candidates followed by 3 consistent ones
        val entries = listOf(
            TimeMappingEntry(playbackTime = 2.0, referenceTime = 500.0, score = 0.7f),
            TimeMappingEntry(playbackTime = 4.0, referenceTime = 600.0, score = 0.7f),
            TimeMappingEntry(playbackTime = 10.0, referenceTime = 10.0, score = 0.9f),
            TimeMappingEntry(playbackTime = 12.0, referenceTime = 12.0, score = 0.9f),
            TimeMappingEntry(playbackTime = 14.0, referenceTime = 14.0, score = 0.9f),
        )
        manager.stubMatches(entries)

        val rejections = manager.debugRejectionsSnapshot
        // The noise candidates should be recorded as rejections
        assertTrue("Expected rejections from pre-bootstrap evictions", rejections.isNotEmpty())

        // The 3 consistent candidates should be accepted as mappings
        assertNotNull(manager.referenceTime(forPlaybackTimeMs = 10_000))
        assertNotNull(manager.referenceTime(forPlaybackTimeMs = 12_000))
        assertNotNull(manager.referenceTime(forPlaybackTimeMs = 14_000))
    }

    @Test
    fun `rejection cap enforces FIFO at limit`() {
        val cap = FingerprintConstants.DEBUG_REJECTION_CAP
        for (i in 0 until cap + 50) {
            manager.consider(
                TimeMappingEntry(
                    playbackTime = i * 2.0,
                    referenceTime = i * 1000.0,
                    score = 0.8f,
                ),
            )
        }

        val rejections = manager.debugRejectionsSnapshot
        assertTrue("Rejections should not exceed cap", rejections.size <= cap)
    }
}
