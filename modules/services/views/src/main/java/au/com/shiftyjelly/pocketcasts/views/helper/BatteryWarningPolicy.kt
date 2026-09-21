package au.com.shiftyjelly.pocketcasts.views.helper

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.SystemBatteryRestrictions
import javax.inject.Inject
import javax.inject.Singleton

// Decides when to nag a user whose battery settings can stop playback: a few times per install, at most once per app open.
@Singleton
class BatteryWarningPolicy @Inject constructor(
    private val settings: Settings,
    private val systemBatteryRestrictions: SystemBatteryRestrictions,
) {
    // Read from the main thread and from the streaming warning's background scope.
    @Volatile
    private var shownThisAppOpen = false

    private val timesLeftToShow: Int
        // Clamped because older versions added two warnings per playback failure with no upper bound.
        get() = settings.getTimesToShowBatteryWarning().coerceAtMost(MAX_WARNINGS)

    fun shouldShowWarning(): Boolean {
        if (shownThisAppOpen || systemBatteryRestrictions.isUnrestricted()) {
            return false
        }
        return timesLeftToShow > 0
    }

    fun onWarningShown() {
        shownThisAppOpen = true
        settings.setTimesToShowBatteryWarning(timesLeftToShow - 1)
    }

    private companion object {
        const val MAX_WARNINGS = 3
    }
}
