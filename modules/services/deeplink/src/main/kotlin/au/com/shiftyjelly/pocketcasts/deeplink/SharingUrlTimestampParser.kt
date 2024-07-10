package au.com.shiftyjelly.pocketcasts.deeplink

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class SharingUrlTimestampParser {
    companion object {
        val intervalPattern = Regex("""^(\d+\.?\d*)$""")
        val hmsPattern = Regex("""^(?:(\d+)h)?(?:(0?[0-5]?\d)m)?(?:(0?[0-5]?\d)s)?$""")
        val hhmmssFractionPattern = Regex("""^(?:(\d+):)?(0[0-9]|[1-5][0-9]):(0[0-9]|[1-5][0-9])(?:\.(\d+))?$""")
    }

    fun parseTimestamp(timestamp: String): Pair<Duration?, Duration?>? {
        val splitTimestamps = timestamp.split(",")
        return when (splitTimestamps.size) {
            1 -> timestamp.toDuration() to null
            2 -> {
                val (rawStart, rawEnd) = timestamp.split(",")
                val start = rawStart.toDuration()
                val end = rawEnd.takeIf(String::isNotBlank)?.toDuration()
                start to end
            }
            else -> null
        }?.let { (first, second) -> if (first == null && second == null) null else first to second }
    }

    private fun String.toDuration(): Duration? {
        hmsPattern.matchEntire(this)?.let { match ->
            val h = match.groups[1]?.value?.toLongOrNull() ?: 0
            val m = match.groups[2]?.value?.toLongOrNull() ?: 0
            val s = match.groups[3]?.value?.toDoubleOrNull() ?: 0.0
            return h.hours + m.minutes + s.seconds
        }

        hhmmssFractionPattern.matchEntire(this)?.let { match ->
            val hh = match.groups[1]?.value?.toLongOrNull() ?: 0
            val mm = match.groups[2]?.value?.toLongOrNull() ?: 0
            val ss = match.groups[3]?.value?.toLongOrNull() ?: 0
            val fraction = match.groups[4]?.value?.take(3)?.toDoubleOrNull() ?: 0.0
            return hh.hours + mm.minutes + ss.seconds + fraction.milliseconds
        }

        intervalPattern.matchEntire(this)?.let { match ->
            val interval = match.groups[1]?.value?.toDoubleOrNull() ?: 0.0
            return interval.seconds
        }

        return null
    }
}
