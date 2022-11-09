package au.com.shiftyjelly.pocketcasts.models.db.helper

data class LongestEpisode(
    val title: String,
    val duration: Double,
    val podcastUuid: String,
    val podcastTitle: String,
)
