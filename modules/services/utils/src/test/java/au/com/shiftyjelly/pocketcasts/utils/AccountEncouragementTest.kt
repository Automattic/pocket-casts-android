package au.com.shiftyjelly.pocketcasts.utils

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountEncouragementTest {
    private val interval: Duration = Duration.ofDays(60)
    private val now: Instant = Instant.ofEpochSecond(1_700_000_000)

    @Test
    fun `waits when not eligible`() {
        val decision = AccountEncouragement.decide(
            isEligible = false,
            lastShown = now.minus(interval.multipliedBy(2)),
            now = now,
            interval = interval,
        )

        assertEquals(AccountEncouragement.Decision.Wait, decision)
    }

    @Test
    fun `shows on first eligible launch`() {
        val decision = AccountEncouragement.decide(
            isEligible = true,
            lastShown = null,
            now = now,
            interval = interval,
        )

        assertEquals(AccountEncouragement.Decision.Show, decision)
    }

    @Test
    fun `waits before interval elapses`() {
        val decision = AccountEncouragement.decide(
            isEligible = true,
            lastShown = now.minus(interval).plusSeconds(1),
            now = now,
            interval = interval,
        )

        assertEquals(AccountEncouragement.Decision.Wait, decision)
    }

    @Test
    fun `shows when interval elapsed`() {
        val decision = AccountEncouragement.decide(
            isEligible = true,
            lastShown = now.minus(interval),
            now = now,
            interval = interval,
        )

        assertEquals(AccountEncouragement.Decision.Show, decision)
    }

    @Test
    fun `shows when interval well exceeded`() {
        val decision = AccountEncouragement.decide(
            isEligible = true,
            lastShown = now.minus(interval.multipliedBy(3)),
            now = now,
            interval = interval,
        )

        assertEquals(AccountEncouragement.Decision.Show, decision)
    }

    @Test
    fun `shows when anchor is in the future`() {
        // A future anchor means a backwards device clock or a restored skewed backup.
        val decision = AccountEncouragement.decide(
            isEligible = true,
            lastShown = now.plus(interval),
            now = now,
            interval = interval,
        )

        assertEquals(AccountEncouragement.Decision.Show, decision)
    }

    @Test
    fun `default interval is 60 days`() {
        assertEquals(Duration.ofDays(60), AccountEncouragement.interval)
    }
}
