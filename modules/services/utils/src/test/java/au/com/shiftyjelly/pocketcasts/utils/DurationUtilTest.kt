package au.com.shiftyjelly.pocketcasts.utils

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Test

class DurationUtilTest {
    @Test
    fun `zero duration to HhMmSs`() {
        val text = Duration.ZERO.toHhMmSs()

        assertEquals("00:00", text)
    }

    @Test
    fun `duration to MmSs`() {
        val duration = 15.minutes + 7.seconds

        val text = duration.toHhMmSs()

        assertEquals("15:07", text)
    }

    @Test
    fun `duration to max MmSs`() {
        val duration = 59.minutes + 59.seconds

        val text = duration.toHhMmSs()

        assertEquals("59:59", text)
    }

    @Test
    fun `duration to min HhMmSs`() {
        val duration = 1.hours

        val text = duration.toHhMmSs()

        assertEquals("01:00:00", text)
    }

    @Test
    fun `duration to HhMmSs`() {
        val duration = 12.hours + 5.minutes + 6.seconds

        val text = duration.toHhMmSs()

        assertEquals("12:05:06", text)
    }

    @Test
    fun `duration to long HhMmSs`() {
        val duration = 3478.hours + 42.minutes + 24.seconds

        val text = duration.toHhMmSs()

        assertEquals("3478:42:24", text)
    }
}
