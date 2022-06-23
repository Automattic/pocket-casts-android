package au.com.shiftyjelly.pocketcasts.utils

import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.io.IOException
import java.util.concurrent.CancellationException
import javax.net.ssl.SSLException

object CrashlyticsHelper {

    const val KEY_LAST_ACTIVITY = "last_activity"
    const val KEY_LAST_FRAGMENT = "last_fragment"
    const val KEY_LOCALE = "locale"

    fun logLastActivity(logObject: Any) {
        FirebaseCrashlytics.getInstance().setCustomKey(KEY_LAST_ACTIVITY, logObject.javaClass.name)
    }

    fun logLastFragment(logObject: Any) {
        FirebaseCrashlytics.getInstance().setCustomKey(KEY_LAST_FRAGMENT, logObject.javaClass.name)
    }

    fun recordException(throwable: Throwable) {
        if (shouldIgnoreExceptions(throwable)) {
            return
        }
        FirebaseCrashlytics.getInstance().recordException(throwable)
    }

    fun recordException(message: String, throwable: Throwable) {
        recordException(Exception(message, throwable))
    }

    fun shouldIgnoreExceptions(throwable: Throwable): Boolean {
        if (shouldIgnoreException(throwable)) {
            return true
        }

        var nextCause: Throwable?
        var previousCause = throwable
        while (previousCause.cause.also { nextCause = it } != null && nextCause !== previousCause) {
            previousCause = nextCause as Throwable
            if (shouldIgnoreException(previousCause)) {
                return true
            }
        }
        return false
    }

    private fun shouldIgnoreException(throwable: Throwable): Boolean {
        return (
            // ignore worker job cancels as they will retry
            throwable is CancellationException ||
                // ignore exceptions such as SocketTimeoutException, SocketException or UnknownHostException, as with episode urls we don't control this
                throwable is IOException ||
                // ignore producer certificate exceptions
                throwable is SSLException
            )
    }
}
