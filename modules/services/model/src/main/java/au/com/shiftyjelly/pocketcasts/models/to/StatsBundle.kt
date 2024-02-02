package au.com.shiftyjelly.pocketcasts.models.to

import java.util.Date

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
