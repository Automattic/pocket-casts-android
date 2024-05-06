package au.com.shiftyjelly.pocketcasts.repositories.user

import au.com.shiftyjelly.pocketcasts.crashlogging.CrashReportPermissionCheck
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import javax.inject.Inject

class UserSettingsCrashReportPermission @Inject constructor(
    private val settings: Settings,
) : CrashReportPermissionCheck {
    override fun invoke(): Boolean {
        return settings.sendCrashReports.value
    }
}
