package au.com.shiftyjelly.pocketcasts.utils.log

import android.util.Log

class LogBufferUncaughtExceptionHandler(private val defaultHandler: Thread.UncaughtExceptionHandler) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            LogBuffer.logException(LogBuffer.TAG_CRASH, throwable, "Fatal crash.")
        } catch (throwable: Throwable) {
            Log.e("POCKETCASTS", "Logging crash", throwable)
        }

        defaultHandler.uncaughtException(thread, throwable)
    }
}
