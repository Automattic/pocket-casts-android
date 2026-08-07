package au.com.shiftyjelly.pocketcasts.nowplaying

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TvPlayerEffectsControlsTest {

    @Test
    fun `speed options cover half to triple speed in tenth steps`() {
        assertEquals(26, tvPlaybackSpeedOptions.size)
        assertEquals(0.5, tvPlaybackSpeedOptions.first(), 0.0)
        assertEquals(3.0, tvPlaybackSpeedOptions.last(), 0.0)
        tvPlaybackSpeedOptions.zipWithNext { previous, next ->
            assertEquals(0.1, next - previous, 1e-9)
        }
    }

    @Test
    fun `speed options match their rounded representation`() {
        assertTrue(tvPlaybackSpeedOptions.all { it == (it * 10).toInt() / 10.0 })
    }

    @Test
    fun `speed labels use one decimal place`() {
        assertEquals("0.5x", playbackSpeedLabel(0.5))
        assertEquals("1.0x", playbackSpeedLabel(1.0))
        assertEquals("1.2x", playbackSpeedLabel(1.2))
        assertEquals("3.0x", playbackSpeedLabel(3.0))
    }
}
