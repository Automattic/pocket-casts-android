package au.com.shiftyjelly.pocketcasts.utils

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class SharingUrlTimestampParser {
    fun parseTimestamp(t: String): Pair<Duration?, Duration?> {
        val intervalPattern = Regex("""(\d*\.?\d*)?,?(\d*\.?\d*)?""") // t=10,20 or t=,20 or t=10

        intervalPattern.find(t)?.let { match ->
            val startTime = match.groups[1]?.value?.takeIf { it.isNotEmpty() }?.toDouble()?.toInt() ?: 0
            val endTime = match.groups[2]?.value?.takeIf { it.isNotEmpty() }?.toDouble()?.toInt() ?: 0
            return Pair(startTime.seconds, endTime.seconds)
        }

        return Pair(null, null)
    }
}
