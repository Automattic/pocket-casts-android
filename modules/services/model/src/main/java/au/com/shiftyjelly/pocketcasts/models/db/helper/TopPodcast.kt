package au.com.shiftyjelly.pocketcasts.models.db.helper

data class TopPodcast(
    val uuid: String,
    val title: String,
    val author: String,
    val numberOfPlayedEpisodes: Int,
    val totalPlayedTime: Double,
)
