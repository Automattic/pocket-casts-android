package au.com.shiftyjelly.pocketcasts.models.to

data class Transcript(
    val episodeUuid: String,
    val url: String,
    val type: String,
    val language: String? = null,
)
