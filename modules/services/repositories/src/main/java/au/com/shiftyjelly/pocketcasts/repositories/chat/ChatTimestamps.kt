package au.com.shiftyjelly.pocketcasts.repositories.chat

import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper

data class ChatTimestamp(
    val range: IntRange,
    val startMs: Int,
)

fun findChatTimestamps(text: CharSequence): List<ChatTimestamp> = TimestampPattern.findAll(text)
    .mapNotNull { match ->
        val startMs = match.startClock()?.let(::parseClockMs) ?: return@mapNotNull null
        ChatTimestamp(match.range, startMs)
    }
    .toList()

fun formatChatTimestamp(ms: Int): String = TimeHelper.formattedSeconds(ms / 1000.0, noHoursFormat = "%d:%02d")

fun formatChatTimestampRange(startMs: Int, endMs: Int) = "${formatChatTimestamp(startMs)} - ${formatChatTimestamp(endMs)}"

internal fun String.withChatTimestamps(): String = TimestampPattern.replace(this) { match ->
    val startMs = match.startClock()?.let(::parseClockMs)
    val endClock = match.endClock()
    val endMs = endClock?.let(::parseClockMs)
    when {
        startMs == null || (endClock != null && endMs == null) -> match.value
        endMs == null -> "(${formatChatTimestamp(startMs)})"
        else -> "(${formatChatTimestampRange(startMs, endMs)})"
    }
}

private fun MatchResult.startClock() = (groups[1] ?: groups[3])?.value

private fun MatchResult.endClock() = (groups[2] ?: groups[4])?.value

private fun parseClockMs(value: String): Int? {
    val clock = value.substringBefore('.').substringBefore(',')
    val millis = value.substringAfter('.', "").ifEmpty { value.substringAfter(',', "") }
        .padEnd(3, '0')
        .take(3)
        .toIntOrNull() ?: 0
    val parts = clock.split(':').map { it.toIntOrNull() ?: return null }
    val (hours, minutes, seconds) = if (parts.size == 3) parts else listOf(0) + parts
    if (seconds >= 60 || (parts.size == 3 && minutes >= 60)) return null
    return (hours * 3600 + minutes * 60 + seconds) * 1000 + millis
}

private const val CLOCK = """(?<![\d:.,])(?:\d{1,2}:)?\d{1,2}:\d{2}(?:[.,]\d{1,3})?(?![\d:])(?!\s?[aApP]\.?[mM]\b)"""
private const val SEPARATOR = """(?:\s*[–—-]\s*|\s+to\s+)"""
private val TimestampPattern = Regex("""\(\s*($CLOCK)(?:$SEPARATOR($CLOCK))?\s*\)|($CLOCK)(?:$SEPARATOR($CLOCK))?""")
