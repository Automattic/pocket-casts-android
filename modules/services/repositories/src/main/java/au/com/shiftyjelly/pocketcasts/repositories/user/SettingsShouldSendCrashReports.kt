package au.com.shiftyjelly.pocketcasts.repositories.user

import au.com.shiftyjelly.pocketcasts.crashlogging.ShouldSendCrashReports
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import javax.inject.Inject

class SettingsShouldSendCrashReports @Inject constructor(
    private val settings: Settings,
) : ShouldSendCrashReports {
    override fun invoke(): Boolean {
        return settings.sendCrashReports.value
    }
}
