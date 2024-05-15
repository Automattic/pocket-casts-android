package au.com.shiftyjelly.pocketcasts.utils

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class SharingUrlTimestampParser {
    fun parseTimestamp(t: String): Pair<Duration?, Duration?> {
        val intervalPattern = Regex("""(\d*\.?\d*)?,?(\d*\.?\d*)?""") // t=10,20 or t=,20 or t=10
        val hmsPattern = Regex("""(?:(\d+)h)?(?:(\d+)m)?(?:(\d+(?:\.\d+)?)s)?""") // t=10h20m30s or t=10m30s or or 10m or t=10s

        hmsPattern.matchEntire(t)?.let { match ->
            val hours = match.groups[1]?.value?.toInt() ?: 0
            val minutes = match.groups[2]?.value?.toInt() ?: 0
            val seconds = match.groups[3]?.value?.toDouble() ?: 0.0
            val totalSeconds = ((hours * 3600) + (minutes * 60) + seconds)
            return Pair(totalSeconds.seconds, null)
        }

        intervalPattern.find(t)?.let { match ->
            val startTime = match.groups[1]?.value?.takeIf { it.isNotEmpty() }?.toDouble()?.toInt() ?: 0
            val endTime = match.groups[2]?.value?.takeIf { it.isNotEmpty() }?.toDouble()?.toInt() ?: 0
            return Pair(startTime.seconds, endTime.seconds)
        }

        return Pair(null, null)
    }
}
