package au.com.shiftyjelly.pocketcasts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PocketCastsMediaIntentReceiverTest {
    @Test
    fun `seek target moves back by the rewind delta`() {
        assertEquals(50_000L, seekTargetPositionMs(approximatePositionMs = 60_000L, durationMs = 120_000L, deltaMs = -10_000L, isLiveStream = false, isPlayingAd = false))
    }

    @Test
    fun `seek target moves forward by the forward delta`() {
        assertEquals(90_000L, seekTargetPositionMs(approximatePositionMs = 60_000L, durationMs = 120_000L, deltaMs = 30_000L, isLiveStream = false, isPlayingAd = false))
    }

    @Test
    fun `seek target does not go before the start`() {
        assertEquals(0L, seekTargetPositionMs(approximatePositionMs = 3_000L, durationMs = 120_000L, deltaMs = -10_000L, isLiveStream = false, isPlayingAd = false))
    }

    @Test
    fun `seek target does not go past the end`() {
        assertEquals(120_000L, seekTargetPositionMs(approximatePositionMs = 110_000L, durationMs = 120_000L, deltaMs = 30_000L, isLiveStream = false, isPlayingAd = false))
    }

    @Test
    fun `seek target is not capped when the duration is unknown`() {
        assertEquals(140_000L, seekTargetPositionMs(approximatePositionMs = 110_000L, durationMs = 0L, deltaMs = 30_000L, isLiveStream = false, isPlayingAd = false))
    }

    @Test
    fun `seek target is null when delta is zero`() {
        assertNull(seekTargetPositionMs(approximatePositionMs = 60_000L, durationMs = 120_000L, deltaMs = 0L, isLiveStream = false, isPlayingAd = false))
    }

    @Test
    fun `seek target is null for a live stream`() {
        assertNull(seekTargetPositionMs(approximatePositionMs = 60_000L, durationMs = 120_000L, deltaMs = -10_000L, isLiveStream = true, isPlayingAd = false))
    }

    @Test
    fun `seek target is null when an ad is playing`() {
        assertNull(seekTargetPositionMs(approximatePositionMs = 60_000L, durationMs = 120_000L, deltaMs = -10_000L, isLiveStream = false, isPlayingAd = true))
    }
}
