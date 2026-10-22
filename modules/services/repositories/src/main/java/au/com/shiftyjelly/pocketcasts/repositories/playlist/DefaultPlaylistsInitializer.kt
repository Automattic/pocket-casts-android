package au.com.shiftyjelly.pocketcasts.repositories.playlist

interface DefaultPlaylistsInitializer {
    suspend fun initialize(force: Boolean = false)
}
