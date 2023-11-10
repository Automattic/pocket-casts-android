package au.com.shiftyjelly.pocketcasts.models.db.helper

data class EpisodesStartedAndCompleted(
    val started: Int,
    val completed: Int,
) {
    val percentage: Double = if (started == 0) {
        0.0
    } else {
        (completed.toDouble() / started.toDouble()) * 100.0
    }
}
