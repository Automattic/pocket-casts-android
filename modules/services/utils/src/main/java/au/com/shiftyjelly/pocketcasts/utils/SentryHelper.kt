package au.com.shiftyjelly.pocketcasts.utils

import io.sentry.Sentry
import java.io.IOException
import java.util.concurrent.CancellationException
import javax.net.ssl.SSLException

object SentryHelper {
    const val GLOBAL_TAG_APP_PLATFORM = "app.platform"

    fun recordException(throwable: Throwable) {
        if (shouldIgnoreExceptions(throwable)) {
            return
        }
        Sentry.captureException(throwable)
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

    enum class AppPlatform(val value: String) {
        MOBILE("mobile"),
        AUTOMOTIVE("automotive"),
        WEAR("wear"),
    }
}
