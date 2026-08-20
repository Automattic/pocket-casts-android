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

        /** Don't show, but anchor the cadence clock to now (the first eligible launch). */
        Anchor,

        /** Don't show and leave the clock untouched. */
        Wait,
    }

    /**
     * Pure cadence decision, with no Android/preferences dependencies so it can be unit-tested.
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
        // (Re)anchor when there's no clock yet, or when the stored anchor is in the future — the
        // latter happens if the device clock moves backwards or a skewed backup is restored, and
        // would otherwise suppress the modal indefinitely.
        if (lastShown == null || lastShown.isAfter(now)) return Decision.Anchor
        return if (!now.isBefore(lastShown.plus(interval))) Decision.Show else Decision.Wait
    }
}
