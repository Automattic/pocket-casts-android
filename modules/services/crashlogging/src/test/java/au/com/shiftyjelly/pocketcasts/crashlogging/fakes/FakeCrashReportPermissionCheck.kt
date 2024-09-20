package au.com.shiftyjelly.pocketcasts.crashlogging.fakes

import au.com.shiftyjelly.pocketcasts.crashlogging.CrashReportPermissionCheck

class FakeCrashReportPermissionCheck : CrashReportPermissionCheck {
    override fun invoke(): Boolean = true
}
