package au.com.shiftyjelly.pocketcasts.utils

import io.sentry.Sentry

object SentryHelper {

    fun recordException(throwable: Throwable) {
        Sentry.captureException(throwable)
    }

    fun recordException(message: String, throwable: Throwable) {
        recordException(Exception(message, throwable))
    }
}
