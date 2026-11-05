package au.com.shiftyjelly.pocketcasts.endofyear.playback2026

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearSync
import com.automattic.pocketcasts.playback.PlaybackPayload
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class Playback2026ViewModel @Inject constructor(
    private val endOfYearSync: EndOfYearSync,
    private val endOfYearManager: EndOfYearManager,
) : ViewModel() {
    private val _payload = MutableStateFlow<PlaybackPayload?>(null)

    /** The payload to show, or null while the listening history is synced and read. */
    val payload: StateFlow<PlaybackPayload?> = _payload.asStateFlow()

    init {
        viewModelScope.launch {
            // Pulls the year's listening history from the server when signed in, so the stats include
            // episodes played on other devices. Without it, the local history is used.
            if (!endOfYearSync.sync(Playback2026.YEAR)) {
                Timber.i("Playback 2026: listening history not synced, using local data")
            }
            _payload.value = endOfYearManager.getPlaybackStats(Playback2026.YEAR).toPlaybackPayload(Playback2026.YEAR)
        }
    }
}
