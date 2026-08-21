package au.com.shiftyjelly.pocketcasts.utils

import java.time.Duration
import java.time.Instant

/**
 * Cadence logic for the recurring "Encourage Account Creation" modal shown to logged-out users.
 *
 * The modal is shown to logged-out users who have already completed onboarding, first
 * [interval] after the initial eligible launch and every [interval] thereafter. The initial
 * eligible launch only anchors the clock (returns [Decision.Anchor]) so we don't collide with
 * onboarding.
 */
object AccountEncouragement {
    /** How long to wait between showings of the modal (60 days). */
    val interval: Duration = Duration.ofDays(60)

    enum class Decision {
        /** Show the modal now. */
        Show,

        /** Don't show and leave the clock untouched. */
        Wait,
    }

    /**
     * Pure cadence decision, with no Android/preferences dependencies so it can be unit-tested.
     *
     * Shows on the first eligible launch (no anchor yet), once the interval elapses, or when the
     * stored anchor is in the future (backwards clock / skewed-backup restore). The caller records
     * `now` as the new anchor when it shows.
     *
     * @param isEligible whether the user currently qualifies (flag on, logged out, onboarding done).
     * @param lastShown the persisted anchor, or `null` if the clock has never been started.
     * @param now the current instant.
     * @param interval how long to wait between showings.
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
