package au.com.shiftyjelly.pocketcasts.crashlogging

fun interface CrashReportPermissionCheck {
    operator fun invoke(): Boolean
}
