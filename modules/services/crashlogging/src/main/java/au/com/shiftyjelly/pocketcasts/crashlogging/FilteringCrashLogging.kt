package au.com.shiftyjelly.pocketcasts.crashlogging

import com.automattic.android.tracks.crashlogging.CrashLogging

class FilteringCrashLogging(private val crashLogging: CrashLogging) : CrashLogging by crashLogging {

    override fun sendReport(exception: Throwable?, tags: Map<String, String>, message: String?) {
        if (exception != null && ExceptionsFilter.shouldIgnoreExceptions(exception)) {
            return
        }
        crashLogging.sendReport(exception, tags, message)
    }

    override fun recordException(exception: Throwable, category: String?) {
        if (ExceptionsFilter.shouldIgnoreExceptions(exception)) {
            return
        }
        crashLogging.recordException(exception, category)
    }
}
