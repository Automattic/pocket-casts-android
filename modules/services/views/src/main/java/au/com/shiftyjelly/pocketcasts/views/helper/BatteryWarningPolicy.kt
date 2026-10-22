package au.com.shiftyjelly.pocketcasts.views.helper

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.SystemBatteryRestrictions
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryWarningPolicy @Inject constructor(
    private val settings: Settings,
    private val systemBatteryRestrictions: SystemBatteryRestrictions,
) {
    @Volatile
    private var shownThisAppOpen = false

    private val timesLeftToShow: Int
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
