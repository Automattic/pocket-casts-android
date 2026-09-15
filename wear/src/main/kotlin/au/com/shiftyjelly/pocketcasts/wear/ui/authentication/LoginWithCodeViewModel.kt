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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

@HiltViewModel
class LoginWithCodeViewModel @Inject constructor(
    private val syncManager: SyncManager,
    private val podcastManager: PodcastManager,
) : ViewModel() {

    private val retryCount = MutableStateFlow(0)

    // Polling stops shortly after the screen is no longer visible and a fresh code is requested when it returns.
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<DeviceAuthState> = retryCount
        .flatMapLatest {
            deviceAuthFlow(
                syncManager = syncManager,
                signInSource = SignInSource.UserInitiated.Watch,
                isNewAccount = false,
                maxCodeRequests = MAX_CODE_REQUESTS,
            )
        }
        .onEach { state ->
            if (state is DeviceAuthState.Complete) {
                refreshPodcastsAfterSignIn()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = DeviceAuthState.Loading,
        )

    fun retry() {
        retryCount.update { it + 1 }
    }

    // Start the refresh before completing so the logging in screen waits for it instead of closing straight away.
    private suspend fun refreshPodcastsAfterSignIn() = withContext(NonCancellable) {
        try {
            podcastManager.refreshPodcastsAfterSignIn()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "Failed to refresh podcasts after watch code sign in")
        }
    }

    private companion object {
        // Keeping the screen on while showing a code is expensive on a watch, so give up once the first code expires.
        const val MAX_CODE_REQUESTS = 1
    }
}
