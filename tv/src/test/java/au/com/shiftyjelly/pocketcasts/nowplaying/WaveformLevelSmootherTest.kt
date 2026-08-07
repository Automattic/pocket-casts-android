package au.com.shiftyjelly.pocketcasts.nowplaying

import kotlin.math.exp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WaveformLevelSmootherTest {

    private val smoother = WaveformLevelSmoother()

    @Test
    fun `snaps to the target on the first sample`() {
        assertEquals(0.42f, smoother.smooth(0.42f, 1.0), 0f)
    }

    @Test
    fun `snaps to the target when time does not advance`() {
        smoother.smooth(0f, 1.0)
        assertEquals(1f, smoother.smooth(1f, 1.0), 0f)
    }

    @Test
    fun `rises with the attack time constant`() {
        smoother.smooth(0f, 0.0)
        val expected = 1f - exp(-0.05 / 0.05).toFloat()
        assertEquals(expected, smoother.smooth(1f, 0.05), 1e-4f)
    }

    @Test
    fun `falls with the slower release time constant`() {
        smoother.smooth(1f, 0.0)
        val expected = 1f - (1f - exp(-0.05 / 0.3).toFloat())
        assertEquals(expected, smoother.smooth(0f, 0.05), 1e-4f)
    }

    @Test
    fun `attack converges faster than release`() {
        val rising = WaveformLevelSmoother()
        rising.smooth(0f, 0.0)
        val risen = rising.smooth(1f, 0.05)

        val falling = WaveformLevelSmoother()
        falling.smooth(1f, 0.0)
        val fallen = falling.smooth(0f, 0.05)

        assertTrue(risen > 1f - fallen)
    }

    @Test
    fun `caps the elapsed time between samples`() {
        smoother.smooth(0f, 0.0)
        val expected = 1f - exp(-0.1 / 0.05).toFloat()
        assertEquals(expected, smoother.smooth(1f, 5.0), 1e-4f)
    }
}
