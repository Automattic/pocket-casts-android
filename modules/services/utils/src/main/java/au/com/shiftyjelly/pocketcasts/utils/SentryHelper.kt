package au.com.shiftyjelly.pocketcasts.utils

import io.sentry.Sentry

object SentryHelper {

    fun recordException(throwable: Throwable) {
        // TODO
//        if (shouldIgnoreExceptions(throwable)) {
//            return
//        }
        Sentry.captureException(throwable)
    }

    fun recordException(message: String, throwable: Throwable) {
        recordException(Exception(message, throwable))
    }
}
