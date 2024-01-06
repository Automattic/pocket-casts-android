package au.com.shiftyjelly.pocketcasts.models.to

import java.util.Date
import kotlin.time.Duration

// Prefer using StatsBundleData instead of StatsBundle. Eventually,
// it would be nice to remove StatsBundle entirely.
data class StatsBundle(
    val values: Map<String, Long>,
    val startedAt: Date?,
) {
    companion object {
        const val SERVER_KEY_SILENCE_REMOVAL = "timeSilenceRemoval"
        const val SERVER_KEY_SKIPPING = "timeSkipping"
        const val SERVER_KEY_AUTO_SKIPPING = "timeIntroSkipping"
        const val SERVER_KEY_VARIABLE_SPEED = "timeVariableSpeed"
        const val SERVER_KEY_TOTAL_LISTENED = "timeListened"
        const val SERVER_KEY_STARTED_AT = "timesStartedAt"
    }
}

data class StatsBundleData(
    val timeSilenceRemoval: Duration,
    val timeSkipping: Duration,
    val timeIntroSkipping: Duration,
    val timeVariableSpeed: Duration,
    val timeListened: Duration,
    val startedAt: Date?,
) {
    fun toStatsBundle() = StatsBundle(
        values = buildMap {
            put(StatsBundle.SERVER_KEY_SILENCE_REMOVAL, timeSilenceRemoval.inWholeSeconds)
            put(StatsBundle.SERVER_KEY_SKIPPING, timeSkipping.inWholeSeconds)
            put(StatsBundle.SERVER_KEY_AUTO_SKIPPING, timeIntroSkipping.inWholeSeconds)
            put(StatsBundle.SERVER_KEY_VARIABLE_SPEED, timeVariableSpeed.inWholeSeconds)
            put(StatsBundle.SERVER_KEY_TOTAL_LISTENED, timeListened.inWholeSeconds)
            if (startedAt != null) {
                put(StatsBundle.SERVER_KEY_STARTED_AT, startedAt.time)
            }
        },
        startedAt = startedAt,
    )
}
