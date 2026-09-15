package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.DeviceAuthState
import au.com.shiftyjelly.pocketcasts.repositories.sync.SignInSource
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.deviceAuthFlow
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LoginWithCodeViewModel @Inject constructor(
    private val syncManager: SyncManager,
    private val podcastManager: PodcastManager,
) : ViewModel() {

    private val _state = MutableStateFlow<DeviceAuthState>(DeviceAuthState.Loading)
    val state: StateFlow<DeviceAuthState> = _state.asStateFlow()

    private var pollingJob: Job? = null

    init {
        requestDeviceCode()
    }

    fun retry() {
        requestDeviceCode()
    }

    private fun requestDeviceCode() {
        pollingJob?.cancel()
        _state.value = DeviceAuthState.Loading
        pollingJob = viewModelScope.launch {
            deviceAuthFlow(
                syncManager = syncManager,
                signInSource = SignInSource.UserInitiated.Watch,
                isNewAccount = false,
            ).collect { state ->
                if (state is DeviceAuthState.Complete) {
                    refreshPodcastsAfterSignIn()
                }
                _state.value = state
            }
        }
    }

    // Start the refresh before completing so the logging in screen waits for it instead of closing straight away.
    private suspend fun refreshPodcastsAfterSignIn() {
        try {
            podcastManager.refreshPodcastsAfterSignIn()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "Failed to refresh podcasts after watch code sign in")
        }
    }
}
