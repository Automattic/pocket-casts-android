package au.com.shiftyjelly.pocketcasts.repositories.playlist

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class DefaultPlaylistsInitializater @Inject constructor(
    private val settings: Settings,
    private val playlistManager: PlaylistManager,
) {
    private val mutex = Mutex()

    suspend fun initialize(force: Boolean = false) = mutex.withLock {
        if (force || !settings.getBooleanForKey(CREATED_DEFAULT_PLAYLISTS_KEY, false)) {
            playlistManager.upsertSmartPlaylist(SmartPlaylistDraft.NewReleases)
            playlistManager.upsertSmartPlaylist(SmartPlaylistDraft.InProgress)
            settings.setBooleanForKey(CREATED_DEFAULT_PLAYLISTS_KEY, true)
        }
    }

    companion object {
        private const val CREATED_DEFAULT_PLAYLISTS_KEY = "createdDefaultPlaylists"
    }
}
