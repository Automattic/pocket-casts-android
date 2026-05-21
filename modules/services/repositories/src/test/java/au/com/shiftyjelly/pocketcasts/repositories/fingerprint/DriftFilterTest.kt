package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager.TimeMappingEntry
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class DriftFilterTest {

    private lateinit var manager: FingerprintTimingManager

    @Before
    fun setUp() {
        manager = FingerprintTimingManager(
            playbackManager = mock(PlaybackManager::class.java),
            referenceRetriever = mock(FingerprintReferenceRetriever::class.java),
        )
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
}
