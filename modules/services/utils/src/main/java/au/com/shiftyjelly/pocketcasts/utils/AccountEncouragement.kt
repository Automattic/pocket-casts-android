package au.com.shiftyjelly.pocketcasts.utils

import java.time.Duration
import java.time.Instant

/** Cadence logic for the recurring account-encouragement modal, kept Android-free so it can be unit-tested. */
object AccountEncouragement {
    val interval: Duration = Duration.ofDays(60)

    enum class Decision {
        Show,
        Wait,
    }

    /**
     * Callers record [now] as the new anchor whenever this returns [Decision.Show].
     * A [lastShown] in the future (backwards clock / skewed-backup restore) shows rather than suppressing indefinitely.
     */
    fun decide(
        isEligible: Boolean,
        lastShown: Instant?,
        now: Instant,
        interval: Duration = this.interval,
    ): Decision {
        if (!isEligible) return Decision.Wait
        if (lastShown == null || lastShown.isAfter(now)) return Decision.Show
        return if (!now.isBefore(lastShown.plus(interval))) Decision.Show else Decision.Wait
    }
}
