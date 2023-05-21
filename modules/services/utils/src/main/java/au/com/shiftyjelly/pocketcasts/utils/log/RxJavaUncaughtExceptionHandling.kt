package au.com.shiftyjelly.pocketcasts.utils.log

import au.com.shiftyjelly.pocketcasts.helper.BuildConfig
import io.reactivex.plugins.RxJavaPlugins
import io.sentry.Sentry

// Wrapper that indicates this exception is only thrown in debug builds.
// We still want to try to fix any of these that occur, but they won't
// crash the app in release builds.
class DebugOnlyException(t: Throwable) :
    Throwable("Exception that is only thrown in debug builds.", t)

object RxJavaUncaughtExceptionHandling {
    fun setUp() {

        // RxJava's default error handler crashes the app on any uncaught exception. Allow
        // that on debug builds, but swallow the error in release builds.
        // See https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0#error-handling
        RxJavaPlugins.setErrorHandler { exception ->
            if (BuildConfig.DEBUG) {
                throw DebugOnlyException(exception)
            } else {
                Sentry.captureException(exception)
                LogBuffer.e(
                    tag = LogBuffer.TAG_RX_JAVA_DEFAULT_ERROR_HANDLER,
                    throwable = exception,
                    message = "RxJava default error handler caught an exception"
                )
            }
        }
    }
}
