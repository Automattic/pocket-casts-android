package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager.TimeMappingEntry
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class AdDetectionTest {

    private lateinit var manager: FingerprintTimingManager

    @Before
    fun setUp() {
        val playbackManager = mock(PlaybackManager::class.java)
        whenever(playbackManager.playbackStateFlow).thenReturn(MutableStateFlow(PlaybackState()))
        manager = FingerprintTimingManager(
            playbackManager = playbackManager,
            referenceRetriever = mock(FingerprintReferenceRetriever::class.java),
        )
    }

    @Test
    fun `ad not flagged when not yet active`() {
        manager.setProcessedRange(0.0, 200.0)
        // hasReachedActive is false by default
        manager.evaluateAdState(100.0)
        assertFalse(manager.isAdInProgress.value)
    }

    @Test
    fun `ad not flagged when position is before processed range`() {
        manager.setHasReachedActive(true)
        manager.setProcessedRange(10.0, 200.0)
        manager.insert(TimeMappingEntry(playbackTime = 30.0, referenceTime = 30.0))

        manager.evaluateAdState(5.0)
        assertFalse(manager.isAdInProgress.value)
    }

    @Test
    fun `ad not flagged when position is after processed range`() {
        manager.setHasReachedActive(true)
        manager.setProcessedRange(0.0, 100.0)
        manager.insert(TimeMappingEntry(playbackTime = 30.0, referenceTime = 30.0))

        manager.evaluateAdState(110.0)
        assertFalse(manager.isAdInProgress.value)
    }

    @Test
    fun `pre-roll ad detected`() {
        manager.setHasReachedActive(true)
        manager.setProcessedRange(0.0, 200.0)
        manager.insert(TimeMappingEntry(playbackTime = 30.0, referenceTime = 30.0))
        manager.insert(TimeMappingEntry(playbackTime = 32.0, referenceTime = 32.0))

        manager.evaluateAdState(10.0)
        assertTrue(manager.isAdInProgress.value)
    }

    @Test
    fun `pre-roll position past first anchor is not ad`() {
        manager.setHasReachedActive(true)
        manager.setProcessedRange(0.0, 200.0)
        manager.insert(TimeMappingEntry(playbackTime = 30.0, referenceTime = 30.0))
        manager.insert(TimeMappingEntry(playbackTime = 32.0, referenceTime = 32.0))

        manager.evaluateAdState(31.0)
        assertFalse(manager.isAdInProgress.value)
    }

    @Test
    fun `mid-roll ad detected`() {
        manager.setHasReachedActive(true)
        manager.setProcessedRange(0.0, 200.0)
        manager.insert(TimeMappingEntry(playbackTime = 40.0, referenceTime = 40.0))
        manager.insert(TimeMappingEntry(playbackTime = 70.0, referenceTime = 55.0))

        manager.evaluateAdState(55.0)
        assertTrue(manager.isAdInProgress.value)
    }

    @Test
    fun `mid-roll position in matched cluster is not ad`() {
        manager.setHasReachedActive(true)
        manager.setProcessedRange(0.0, 200.0)
        manager.insert(TimeMappingEntry(playbackTime = 10.0, referenceTime = 10.0))
        manager.insert(TimeMappingEntry(playbackTime = 12.0, referenceTime = 12.0))
        // 16s gap
        manager.insert(TimeMappingEntry(playbackTime = 28.0, referenceTime = 28.0))
        manager.insert(TimeMappingEntry(playbackTime = 30.0, referenceTime = 30.0))

        manager.evaluateAdState(11.0)
        assertFalse(manager.isAdInProgress.value)

        manager.evaluateAdState(29.0)
        assertFalse(manager.isAdInProgress.value)
    }

    @Test
    fun `post-roll ad detected`() {
        manager.setHasReachedActive(true)
        manager.setProcessedRange(0.0, 200.0)
        manager.insert(TimeMappingEntry(playbackTime = 168.0, referenceTime = 168.0))
        manager.insert(TimeMappingEntry(playbackTime = 170.0, referenceTime = 170.0))

        manager.evaluateAdState(185.0)
        assertTrue(manager.isAdInProgress.value)
    }

    @Test
    fun `gap exactly at threshold is not ad`() {
        manager.setHasReachedActive(true)
        manager.setProcessedRange(0.0, 100.0)
        manager.insert(TimeMappingEntry(playbackTime = 10.0, referenceTime = 10.0))
        manager.insert(TimeMappingEntry(playbackTime = 22.0, referenceTime = 22.0))

        manager.evaluateAdState(15.0)
        assertFalse(manager.isAdInProgress.value)
    }

    @Test
    fun `gap below threshold is not ad`() {
        manager.setHasReachedActive(true)
        manager.setProcessedRange(0.0, 100.0)
        manager.insert(TimeMappingEntry(playbackTime = 10.0, referenceTime = 10.0))
        manager.insert(TimeMappingEntry(playbackTime = 20.0, referenceTime = 20.0))

        manager.evaluateAdState(15.0)
        assertFalse(manager.isAdInProgress.value)
    }

    @Test
    fun `ad clears when anchors fill the gap`() {
        manager.setHasReachedActive(true)
        manager.setProcessedRange(0.0, 200.0)
        manager.insert(TimeMappingEntry(playbackTime = 40.0, referenceTime = 40.0))
        manager.insert(TimeMappingEntry(playbackTime = 70.0, referenceTime = 55.0))

        manager.evaluateAdState(55.0)
        assertTrue(manager.isAdInProgress.value)

        manager.insert(TimeMappingEntry(playbackTime = 50.0, referenceTime = 45.0))
        manager.insert(TimeMappingEntry(playbackTime = 60.0, referenceTime = 50.0))

        manager.evaluateAdState(55.0)
        assertFalse(manager.isAdInProgress.value)
    }

    @Test
    fun `no anchors with small processed range is not ad`() {
        manager.setHasReachedActive(true)
        manager.setProcessedRange(0.0, 10.0)

        manager.evaluateAdState(5.0)
        assertFalse(manager.isAdInProgress.value)
    }

    @Test
    fun `no anchors with large processed range is ad`() {
        manager.setHasReachedActive(true)
        manager.setProcessedRange(0.0, 20.0)

        manager.evaluateAdState(10.0)
        assertTrue(manager.isAdInProgress.value)
    }
}
