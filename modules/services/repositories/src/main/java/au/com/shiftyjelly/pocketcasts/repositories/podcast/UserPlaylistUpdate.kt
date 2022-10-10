package au.com.shiftyjelly.pocketcasts.repositories.podcast

data class UserPlaylistUpdate(
    val properties: List<PlaylistProperty>,
    val source: PlaylistUpdateSource
) {
    init {
        if (properties.isEmpty()) {
            throw IllegalStateException("UserPlaylistUpdates cannot have an empty list of properties")
        }
    }
}
