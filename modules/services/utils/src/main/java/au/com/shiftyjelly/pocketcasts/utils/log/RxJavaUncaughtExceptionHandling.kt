package au.com.shiftyjelly.pocketcasts.utils.log

import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import timber.log.Timber

object RxJavaUncaughtExceptionHandling {
    fun setUp() {
        RxJavaPlugins.setErrorHandler { exception ->
            when (exception) {

                is UndeliverableException -> {
                    // Merely log undeliverable exceptions
                    Timber.w(exception)
                    LogBuffer.i(LogBuffer.TAG_RX_JAVA_DEFAULT_ERROR_HANDLER, "Caught undeliverable exception: ${exception.cause}")
                }

                else -> {
                    Thread.currentThread().also { thread ->
                        thread.uncaughtExceptionHandler?.uncaughtException(thread, exception)
                    }
                }
            }
        }
    }
}
