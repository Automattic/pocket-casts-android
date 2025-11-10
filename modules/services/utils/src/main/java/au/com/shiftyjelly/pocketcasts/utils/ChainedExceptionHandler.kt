package au.com.shiftyjelly.pocketcasts.utils

class ChainedExceptionHandler(
    val handlers: List<Thread.UncaughtExceptionHandler>,
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        handlers.forEach { handler ->
            runCatching { handler.uncaughtException(thread, throwable) }
        }
    }
}
