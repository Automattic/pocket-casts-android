package au.com.shiftyjelly.pocketcasts.repositories.refresh

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

        clock.advanceBy(RUN_NOW_INTERVAL_MS)

        assertNull(throttle.tryStart(runNow = true))
    }

    @Test
    fun `manual attempt after the run now interval is allowed`() {
        throttle.tryStart(runNow = true)

        clock.advanceBy(RUN_NOW_INTERVAL_MS + 1)

        assertNotNull(throttle.tryStart(runNow = true))
    }

    @Test
    fun `periodic attempt uses the longer periodic interval`() {
        throttle.tryStart(runNow = true)

        clock.advanceBy(RUN_NOW_INTERVAL_MS + 1)

        assertNull(throttle.tryStart(runNow = false))
    }

    @Test
    fun `periodic attempt after the periodic interval is allowed`() {
        throttle.tryStart(runNow = false)

        clock.advanceBy(PERIODIC_INTERVAL_MS + 1)

        assertNotNull(throttle.tryStart(runNow = false))
    }

    @Test
    fun `aborted attempt does not block the next attempt`() {
        val cancelledBackgroundRun = throttle.tryStart(runNow = false)
        clock.advanceBy(100)

        cancelledBackgroundRun?.abort()

        assertNotNull(throttle.tryStart(runNow = true))
    }

    @Test
    fun `aborting restores the stamp of the previous attempt`() {
        throttle.tryStart(runNow = true)
        clock.advanceBy(RUN_NOW_INTERVAL_MS + 1)
        val secondAttempt = throttle.tryStart(runNow = true)

        secondAttempt?.abort()

        // The first attempt is still within the periodic interval, so it keeps throttling periodic runs.
        assertNull(throttle.tryStart(runNow = false))
    }

    @Test
    fun `aborting a stale attempt does not clear a newer attempt`() {
        val staleBackgroundRun = throttle.tryStart(runNow = false)
        clock.advanceBy(RUN_NOW_INTERVAL_MS + 1)
        throttle.tryStart(runNow = true)

        staleBackgroundRun?.abort()

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

        clock.advanceBy(1)

        assertNotNull(unthrottled.tryStart(runNow = true))
    }

    private companion object {
        const val RUN_NOW_INTERVAL_MS = 15_000L
        const val PERIODIC_INTERVAL_MS = 300_000L
    }
}

private class MutableClock(private var now: Instant) : Clock() {
    fun advanceBy(millis: Long) {
        now = now.plusMillis(millis)
    }

    override fun instant(): Instant = now

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this
}
