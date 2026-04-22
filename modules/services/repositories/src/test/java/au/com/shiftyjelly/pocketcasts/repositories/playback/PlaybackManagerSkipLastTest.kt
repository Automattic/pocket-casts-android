package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager.Companion.effectiveSkipLastMs
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager.Companion.shouldSkipLast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackManagerSkipLastTest {

    @Test
    fun `effectiveSkipLastMs at 1x equals configured seconds`() {
        assertEquals(120_000L, effectiveSkipLastMs(skipLastSecs = 120, playbackSpeed = 1.0))
    }

    @Test
    fun `effectiveSkipLastMs at 2x doubles the window`() {
        assertEquals(240_000L, effectiveSkipLastMs(skipLastSecs = 120, playbackSpeed = 2.0))
    }

    @Test
    fun `effectiveSkipLastMs at 1 point 5x scales linearly`() {
        assertEquals(180_000L, effectiveSkipLastMs(skipLastSecs = 120, playbackSpeed = 1.5))
    }

    @Test
    fun `effectiveSkipLastMs below 1x is clamped to 1x`() {
        // Sub-1x users must never lose time they configured; the threshold stays at the 1x window.
        assertEquals(120_000L, effectiveSkipLastMs(skipLastSecs = 120, playbackSpeed = 0.5))
        assertEquals(120_000L, effectiveSkipLastMs(skipLastSecs = 120, playbackSpeed = 0.8))
    }

    @Test
    fun `effectiveSkipLastMs is zero when skipLastSecs is null or zero`() {
        assertEquals(0L, effectiveSkipLastMs(skipLastSecs = null, playbackSpeed = 2.0))
        assertEquals(0L, effectiveSkipLastMs(skipLastSecs = 0, playbackSpeed = 2.0))
    }

    @Test
    fun `shouldSkipLast at 1x does not fire just before the window starts`() {
        // 30m media, skipLast=120s. Window starts at 28:00 (1_680_000ms). One ms before → no fire.
        assertFalse(
            shouldSkipLast(
                skipLastSecs = 120,
                playbackSpeed = 1.0,
                positionMs = 1_679_999,
                durationMs = 1_800_000,
            ),
        )
    }

    @Test
    fun `shouldSkipLast at 1x fires when entering the window`() {
        // 30m media, skipLast=120s. Remaining = 119_999ms < 120_000ms → fire.
        assertTrue(
            shouldSkipLast(
                skipLastSecs = 120,
                playbackSpeed = 1.0,
                positionMs = 1_680_001,
                durationMs = 1_800_000,
            ),
        )
    }

    @Test
    fun `shouldSkipLast at 2x does not fire just before the listening-time window`() {
        // 30m media at 2x with skipLast=120s listening time = 240s media window.
        // Before 26:00 media = still more than 4 listening minutes left, so no fire.
        assertFalse(
            shouldSkipLast(
                skipLastSecs = 120,
                playbackSpeed = 2.0,
                positionMs = 1_559_999,
                durationMs = 1_800_000,
            ),
        )
    }

    @Test
    fun `shouldSkipLast at 2x fires when 2 listening minutes remain`() {
        // 30m media at 2x with skipLast=120s listening time = 240s media window.
        // Remaining = 239_999ms (< 240_000ms threshold) → fire.
        assertTrue(
            shouldSkipLast(
                skipLastSecs = 120,
                playbackSpeed = 2.0,
                positionMs = 1_560_001,
                durationMs = 1_800_000,
            ),
        )
    }

    @Test
    fun `shouldSkipLast at 0 point 5x keeps the 1x window (never shrinks)`() {
        // The user set Skip Last=120s. On 0.5x we must not reduce the media window below 120_000ms.
        // At 120_001ms remaining (position 1_679_999), 1x would NOT fire, so neither should 0.5x.
        assertFalse(
            shouldSkipLast(
                skipLastSecs = 120,
                playbackSpeed = 0.5,
                positionMs = 1_679_999,
                durationMs = 1_800_000,
            ),
        )
        assertTrue(
            shouldSkipLast(
                skipLastSecs = 120,
                playbackSpeed = 0.5,
                positionMs = 1_680_001,
                durationMs = 1_800_000,
            ),
        )
    }

    @Test
    fun `shouldSkipLast returns false when skipLastSecs is zero or null`() {
        assertFalse(
            shouldSkipLast(skipLastSecs = 0, playbackSpeed = 2.0, positionMs = 1_790_000, durationMs = 1_800_000),
        )
        assertFalse(
            shouldSkipLast(skipLastSecs = null, playbackSpeed = 2.0, positionMs = 1_790_000, durationMs = 1_800_000),
        )
    }

    @Test
    fun `shouldSkipLast returns false when durationMs is null or non-positive`() {
        assertFalse(
            shouldSkipLast(skipLastSecs = 120, playbackSpeed = 1.0, positionMs = 1_000, durationMs = null),
        )
        assertFalse(
            shouldSkipLast(skipLastSecs = 120, playbackSpeed = 1.0, positionMs = 1_000, durationMs = 0),
        )
    }

    @Test
    fun `shouldSkipLast returns false when positionMs is negative`() {
        // Player hasn't reported a position yet.
        assertFalse(
            shouldSkipLast(skipLastSecs = 120, playbackSpeed = 1.0, positionMs = -1, durationMs = 1_800_000),
        )
    }

    @Test
    fun `shouldSkipLast returns false when episode is shorter than the skip-last window`() {
        // A 60s episode with skipLast=120s at 1x: the whole episode is inside the window, but we
        // intentionally do NOT fire because the guard `durationMs > thresholdMs` filters it out.
        // (Preserves prior behaviour for very short episodes.)
        assertFalse(
            shouldSkipLast(skipLastSecs = 120, playbackSpeed = 1.0, positionMs = 0, durationMs = 60_000),
        )
        // Same at 2x: 120s episode, 240s window → still filtered out.
        assertFalse(
            shouldSkipLast(skipLastSecs = 120, playbackSpeed = 2.0, positionMs = 0, durationMs = 120_000),
        )
    }
}
