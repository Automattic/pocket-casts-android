package au.com.shiftyjelly.pocketcasts.sharing

import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Test

class ClipRangeTest {
    @Test
    fun `create clip range in the middle of playback position`() {
        val clipRange = Clip.Range.fromPosition(
            playbackPosition = 10.seconds,
            episodeDuration = 20.seconds,
            clipDuration = 10.seconds,
        )

        assertEquals(Clip.Range(5.seconds, 15.seconds), clipRange)
    }

    @Test
    fun `create clip range with default 15 seconds duration`() {
        val clipRange = Clip.Range.fromPosition(
            playbackPosition = 12.5.seconds,
            episodeDuration = 30.seconds,
        )

        assertEquals(Clip.Range(5.seconds, 20.seconds), clipRange)
    }

    @Test
    fun `create clip range with expected start below 0`() {
        val clipRange = Clip.Range.fromPosition(
            playbackPosition = 0.seconds,
            episodeDuration = 20.seconds,
            clipDuration = 10.seconds,
        )

        assertEquals(Clip.Range(0.seconds, 10.seconds), clipRange)
    }

    @Test
    fun `create clip range with expected end above episode duration`() {
        val clipRange = Clip.Range.fromPosition(
            playbackPosition = 20.seconds,
            episodeDuration = 20.seconds,
            clipDuration = 10.seconds,
        )

        assertEquals(Clip.Range(10.seconds, 20.seconds), clipRange)
    }

    @Test
    fun `create clip range with too long duration`() {
        val clipRange = Clip.Range.fromPosition(
            playbackPosition = 10.seconds,
            episodeDuration = 20.seconds,
            clipDuration = 300.seconds,
        )

        assertEquals(Clip.Range(0.seconds, 20.seconds), clipRange)
    }
}
