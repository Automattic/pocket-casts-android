package au.com.shiftyjelly.pocketcasts.payment

import org.junit.Assert.assertEquals

class TestLogger : Logger {
    private val logs = mutableListOf<LogEntry>()

    override fun info(message: String) {
        logs += LogEntry(LogLevel.Info, message)
    }

    override fun warning(message: String) {
        logs += LogEntry(LogLevel.Warning, message)
    }

    override fun error(message: String, exception: Throwable) {
        logs += LogEntry(LogLevel.Error, message)
    }

    fun assertInfos(vararg messages: String) {
        val infoLogs = logs.filter { it.level == LogLevel.Info }.map { it.message }
        assertEquals(messages.toList(), infoLogs)
    }

    fun assertWarnings(vararg messages: String) {
        val warningLogs = logs.filter { it.level == LogLevel.Warning }.map { it.message }
        assertEquals(messages.toList(), warningLogs)
    }

    fun assertNoLogs() {
        assertEquals(emptyList<LogEntry>(), logs)
    }
}

private data class LogEntry(
    val level: LogLevel,
    val message: String,
)

private enum class LogLevel {
    Info,
    Warning,
    Error,
}
