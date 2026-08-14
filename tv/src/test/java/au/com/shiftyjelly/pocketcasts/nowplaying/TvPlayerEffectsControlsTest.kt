package au.com.shiftyjelly.pocketcasts.nowplaying

import java.util.Locale
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
    fun `the nearest option is found for speeds outside the list`() {
        assertEquals(3.0, nearestPlaybackSpeedOption(5.0), 0.0)
        assertEquals(3.0, nearestPlaybackSpeedOption(3.1), 0.0)
        assertEquals(1.2, nearestPlaybackSpeedOption(1.2), 0.0)
        assertEquals(0.5, nearestPlaybackSpeedOption(0.5), 0.0)
    }

    @Test
    fun `speed labels use one decimal place`() {
        assertEquals("0.5x", playbackSpeedLabel(0.5, Locale.US))
        assertEquals("1.0x", playbackSpeedLabel(1.0, Locale.US))
        assertEquals("1.2x", playbackSpeedLabel(1.2, Locale.US))
        assertEquals("3.0x", playbackSpeedLabel(3.0, Locale.US))
    }

    @Test
    fun `speed labels follow the locale`() {
        assertEquals("1,2x", playbackSpeedLabel(1.2, Locale.GERMANY))
    }
}
