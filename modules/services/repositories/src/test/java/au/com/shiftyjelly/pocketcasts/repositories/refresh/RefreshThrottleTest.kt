package au.com.shiftyjelly.pocketcasts.repositories.refresh

import au.com.shiftyjelly.pocketcasts.sharedtest.MutableClock
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RefreshThrottleTest {
    private val clock = MutableClock(Instant.ofEpochMilli(1_000_000))
    private val throttle = RefreshThrottle(
        clock = clock,
        runNowIntervalMs = RUN_NOW_INTERVAL_MS,
        periodicIntervalMs = PERIODIC_INTERVAL_MS,
    )

    @Test
    fun `first attempt is allowed`() {
        assertNotNull(throttle.tryStart(runNow = true))
    }

    @Test
    fun `manual attempt within the run now interval is rejected`() {
        throttle.tryStart(runNow = true)

        clock += RUN_NOW_INTERVAL_MS.milliseconds

        assertNull(throttle.tryStart(runNow = true))
    }

    @Test
    fun `manual attempt after the run now interval is allowed`() {
        throttle.tryStart(runNow = true)

        clock += (RUN_NOW_INTERVAL_MS + 1).milliseconds

        assertNotNull(throttle.tryStart(runNow = true))
    }

    @Test
    fun `periodic attempt uses the longer periodic interval`() {
        throttle.tryStart(runNow = true)

        clock += (RUN_NOW_INTERVAL_MS + 1).milliseconds

        assertNull(throttle.tryStart(runNow = false))
    }

    @Test
    fun `periodic attempt after the periodic interval is allowed`() {
        throttle.tryStart(runNow = false)

        clock += (PERIODIC_INTERVAL_MS + 1).milliseconds

        assertNotNull(throttle.tryStart(runNow = false))
    }

    @Test
    fun `aborted attempt does not block the next attempt`() {
        val cancelledBackgroundRun = throttle.tryStart(runNow = false)!!
        clock += 100.milliseconds

        cancelledBackgroundRun.abort()

        assertNotNull(throttle.tryStart(runNow = true))
    }

    @Test
    fun `aborting restores the stamp of the previous attempt`() {
        throttle.tryStart(runNow = true)
        clock += (RUN_NOW_INTERVAL_MS + 1).milliseconds
        val secondAttempt = throttle.tryStart(runNow = true)!!

        secondAttempt.abort()

        // The first attempt is still within the periodic interval, so it keeps throttling periodic runs.
        assertNull(throttle.tryStart(runNow = false))
    }

    @Test
    fun `aborting a stale attempt does not clear a newer attempt`() {
        val staleBackgroundRun = throttle.tryStart(runNow = false)!!
        clock += (RUN_NOW_INTERVAL_MS + 1).milliseconds
        throttle.tryStart(runNow = true)

        staleBackgroundRun.abort()

        assertNull(throttle.tryStart(runNow = true))
    }

    @Test
    fun `reset allows the next attempt immediately`() {
        throttle.tryStart(runNow = true)

        throttle.reset()

        assertNotNull(throttle.tryStart(runNow = true))
    }

    @Test
    fun `zero intervals never throttle`() {
        val unthrottled = RefreshThrottle(clock = clock, runNowIntervalMs = 0, periodicIntervalMs = 0)
        unthrottled.tryStart(runNow = true)

        clock += 1.milliseconds

        assertNotNull(unthrottled.tryStart(runNow = true))
    }

    @Test
    fun `tryRun runs the block when allowed`() {
        var ran = false

        val result = throttle.tryRun(runNow = true) {
            ran = true
            true
        }

        assertTrue(result)
        assertTrue(ran)
    }

    @Test
    fun `tryRun skips the block when too soon`() {
        throttle.tryStart(runNow = true)
        var ran = false

        val result = throttle.tryRun(runNow = true) {
            ran = true
            true
        }

        assertFalse(result)
        assertFalse(ran)
    }

    @Test
    fun `tryRun keeps throttling after a block that refreshed`() {
        throttle.tryRun(runNow = true) { true }

        assertNull(throttle.tryStart(runNow = true))
    }

    @Test
    fun `tryRun gives the slot back after a block that did not refresh`() {
        throttle.tryRun(runNow = true) { false }

        assertNotNull(throttle.tryStart(runNow = true))
    }

    @Test
    fun `tryRun gives the slot back and rethrows when the block throws`() {
        try {
            throttle.tryRun(runNow = true) { throw IllegalStateException("boom") }
            fail("Expected the exception to be rethrown")
        } catch (e: IllegalStateException) {
            assertEquals("boom", e.message)
        }

        assertNotNull(throttle.tryStart(runNow = true))
    }

    private companion object {
        const val RUN_NOW_INTERVAL_MS = 15_000L
        const val PERIODIC_INTERVAL_MS = 300_000L
    }
}
