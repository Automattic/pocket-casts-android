package au.com.shiftyjelly.pocketcasts.utils

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class SharingUrlTimestampParser {
    companion object {
        val intervalPattern = Regex("""(\d*\.?\d*)?,?(\d*\.?\d*)?""")
        val hmsPattern = Regex("""(?:(\d+)h)?(?:(0?[0-5]?\d)m)?(?:(0?[0-5]?\d)s)?""")
        val hhmmssFractionPattern = Regex("""(?:(\d+):)?(0[0-9]|[1-5][0-9]):(0[0-9]|[1-5][0-9])(?:\.(\d+))?""")
    }

    fun parseTimestamp(timestamp: String): Pair<Duration?, Duration?> {
        if (timestamp.contains(",")) {
            val (part1, part2) = timestamp.split(",")
            val startParsedTime: Pair<Double?, Double?> = parseTime(part1)
            val endParsedTime: Pair<Double?, Double?> = parseTime(part2)
            return Pair(extractTimeFrom(startParsedTime)?.seconds, extractTimeFrom(endParsedTime)?.seconds)
        }
        val result = parseTime(timestamp)
        return Pair(result.first?.seconds, result.second?.seconds)
    }

    private fun parseTime(t: String): Pair<Double?, Double?> {
        hmsPattern.matchEntire(t)?.let { match ->
            val hours = match.groups[1]?.value?.toInt() ?: 0
            val minutes = match.groups[2]?.value?.toInt() ?: 0
            val seconds = match.groups[3]?.value?.toDouble() ?: 0.0
            val totalSeconds = ((hours * 3600) + (minutes * 60) + seconds)
            return Pair(totalSeconds, null)
        }

        hhmmssFractionPattern.matchEntire(t)?.let { match ->
            val hours = match.groups[1]?.value?.toLong() ?: 0
            val minutes = match.groups[2]?.value?.toLong() ?: 0
            val seconds = match.groups[3]?.value?.toLong() ?: 0
            val fractionInMilliseconds = match.groups[4]?.value?.take(3)?.toDouble() ?: 0.0 // Extract first three digits
            val totalSeconds = (hours * 3600) + (minutes * 60) + seconds + (fractionInMilliseconds / 1000.0)
            return Pair(totalSeconds, null)
        }

        intervalPattern.find(t)?.let { match ->
            val startTime = match.groups[1]?.value?.takeIf { it.isNotEmpty() }?.toDouble()?.toInt()?.takeIf { it != 0 }
            val endTime = match.groups[2]?.value?.takeIf { it.isNotEmpty() }?.toDouble()?.toInt()?.takeIf { it != 0 }
            return Pair(startTime?.toDouble(), endTime?.toDouble())
        }

        return Pair(null, null)
    }

    private fun extractTimeFrom(startParsedTime: Pair<Double?, Double?>) = if (startParsedTime.first == null && startParsedTime.second == null) {
        null
    } else if (startParsedTime.first != null) {
        startParsedTime.first
    } else {
        startParsedTime.second
    }
}
