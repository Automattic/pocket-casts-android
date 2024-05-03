package au.com.shiftyjelly.pocketcasts.crashlogging

fun interface ShouldSendCrashReports {
    operator fun invoke(): Boolean
}
