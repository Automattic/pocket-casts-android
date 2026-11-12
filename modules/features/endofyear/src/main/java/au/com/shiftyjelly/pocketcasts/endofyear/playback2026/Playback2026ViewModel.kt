package au.com.shiftyjelly.pocketcasts.endofyear.playback2026

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearSync
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import com.automattic.pocketcasts.playback.PlaybackPayload
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class Playback2026ViewModel @Inject constructor(
    private val endOfYearSync: EndOfYearSync,
    private val endOfYearManager: EndOfYearManager,
    private val syncManager: SyncManager,
) : ViewModel() {
    private val _payload = MutableStateFlow<PlaybackPayload?>(null)

    /** The payload to show, or null while the listening history is synced and read. */
    val payload: StateFlow<PlaybackPayload?> = _payload.asStateFlow()

    init {
        viewModelScope.launch {
            val archetype = async { fetchArchetype() }
            // Pulls the year's listening history from the server when signed in, so the stats include
            // episodes played on other devices. Without it, the local history is used.
            if (!endOfYearSync.sync(Playback2026.YEAR)) {
                Timber.i("Playback 2026: listening history not synced, using local data")
            }
            _payload.value = endOfYearManager.getPlaybackStats(Playback2026.YEAR).toPlaybackPayload(Playback2026.YEAR, archetype.await())
        }
    }

    /** The listener archetype the server works out, or null when it can't be reached: the stories show their default. */
    private suspend fun fetchArchetype(): PlaybackPayload.Archetype? {
        if (!syncManager.isLoggedIn()) return null
        val archetype = try {
            syncManager.getPlayback2026Archetype()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.i(e, "Playback 2026: couldn't get the archetype, the stories show their default")
            return null
        }
        Timber.i("Playback 2026: archetype $archetype")
        return archetype.toPlaybackArchetype()
    }
}
