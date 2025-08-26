package au.com.shiftyjelly.pocketcasts.repositories.playlist

data class PlaylistPreview(
    val uuid: String,
    val title: String,
    val episodeCount: Int,
    val artworkPodcastUuids: List<String>,
    val type: Type,
) {
    enum class Type {
        Manual,
        Smart,
    }
}
