package au.com.shiftyjelly.pocketcasts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PocketCastsMediaIntentReceiverTest {
    @Test
    fun `rewind delta is negative and in milliseconds`() {
        assertEquals(-10_000L, rewindDeltaMs(10))
        assertEquals(-45_000L, rewindDeltaMs(45))
    }

    @Test
    fun `forward delta is positive and in milliseconds`() {
        assertEquals(30_000L, forwardDeltaMs(30))
        assertEquals(60_000L, forwardDeltaMs(60))
    }

    @Test
    fun `zero skip interval produces a zero delta`() {
        assertEquals(0L, rewindDeltaMs(0))
        assertEquals(0L, forwardDeltaMs(0))
    }

    @Test
    fun `seek target moves back by the rewind delta`() {
        assertEquals(50_000L, seekTargetPositionMs(approximatePositionMs = 60_000L, deltaMs = -10_000L, isLiveStream = false, isPlayingAd = false))
    }

    @Test
    fun `seek target moves forward by the forward delta`() {
        assertEquals(90_000L, seekTargetPositionMs(approximatePositionMs = 60_000L, deltaMs = 30_000L, isLiveStream = false, isPlayingAd = false))
    }

    @Test
    fun `seek target is null when delta is zero`() {
        assertNull(seekTargetPositionMs(approximatePositionMs = 60_000L, deltaMs = 0L, isLiveStream = false, isPlayingAd = false))
    }

    @Test
    fun `seek target is null for a live stream`() {
        assertNull(seekTargetPositionMs(approximatePositionMs = 60_000L, deltaMs = -10_000L, isLiveStream = true, isPlayingAd = false))
    }

    @Test
    fun `seek target is null when an ad is playing`() {
        assertNull(seekTargetPositionMs(approximatePositionMs = 60_000L, deltaMs = -10_000L, isLiveStream = false, isPlayingAd = true))
    }
}
