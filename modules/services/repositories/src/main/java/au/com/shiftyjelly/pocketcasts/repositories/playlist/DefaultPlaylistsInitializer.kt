package au.com.shiftyjelly.pocketcasts.repositories.playlist

import android.content.Context
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Singleton
class DefaultPlaylistsInitializer @Inject constructor(
    private val settings: Settings,
    private val playlistManager: PlaylistManager,
    @ApplicationContext private val context: Context,
) {
    private val mutex = Mutex()

    suspend fun initialize(force: Boolean = false) = mutex.withLock {
        if (force || !settings.getBooleanForKey(CREATED_DEFAULT_PLAYLISTS_KEY, false)) {
            val inProgressUuid = playlistManager.createSmartPlaylist(SmartPlaylistDraft.InProgress)
            playlistManager.updateName(inProgressUuid, context.getString(LR.string.filters_title_in_progress))

            val newReleasesUuid = playlistManager.createSmartPlaylist(SmartPlaylistDraft.NewReleases)
            playlistManager.updateName(newReleasesUuid, context.getString(LR.string.filters_title_new_releases))

            settings.setBooleanForKey(CREATED_DEFAULT_PLAYLISTS_KEY, true)
        }
    }

    companion object {
        private const val CREATED_DEFAULT_PLAYLISTS_KEY = "createdDefaultPlaylists"
    }
}
