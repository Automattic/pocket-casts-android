package au.com.shiftyjelly.pocketcasts.utils.log

import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins

object RxJavaUncaughtExceptionHandling {
    fun setUp() {
        RxJavaPlugins.setErrorHandler { exception ->
            when (exception) {

                is UndeliverableException -> {
                    // Merely log undeliverable exceptions
                    LogBuffer.w(LogBuffer.TAG_RX_JAVA_DEFAULT_ERROR_HANDLER, exception, "Caught undeliverable exception.")
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
