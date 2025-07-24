package au.com.shiftyjelly.pocketcasts.repositories.playlist

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import dagger.assisted.Assisted
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
            playlistManager.createPlaylist(PlaylistDraft.NewReleases)
            playlistManager.createPlaylist(PlaylistDraft.InProgress)
            settings.setBooleanForKey(CREATED_DEFAULT_PLAYLISTS_KEY, true)
        }
    }

    companion object {
        private const val CREATED_DEFAULT_PLAYLISTS_KEY = "createdDefaultPlaylists"
    }
}
