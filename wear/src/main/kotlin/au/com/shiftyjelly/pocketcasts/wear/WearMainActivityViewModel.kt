package au.com.shiftyjelly.pocketcasts.wear

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.watchsync.WatchSync
import au.com.shiftyjelly.pocketcasts.account.watchsync.WatchSyncAuthData
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsTask
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.wear.networking.PhoneConnectionMonitor
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.WatchSyncError
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.WatchSyncState
import com.google.android.horologist.auth.data.tokenshare.TokenBundleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.withTimeout
import timber.log.Timber

@HiltViewModel
class WearMainActivityViewModel @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val podcastManager: PodcastManager,
    private val userManager: UserManager,
    private val settings: Settings,
    @ApplicationContext private val context: Context,
    private val tokenBundleRepository: TokenBundleRepository<WatchSyncAuthData?>,
    private val watchSync: WatchSync,
    private val phoneConnectionMonitor: PhoneConnectionMonitor,
) : ViewModel() {

    data class State(
        val showLoggingInScreen: Boolean = false,
        val signInState: SignInState = SignInState.SignedOut,
        val syncState: WatchSyncState = WatchSyncState.Syncing,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private var syncJob: Job? = null
    private var lastRetryTime: Long = 0L

    init {
        startSyncFlow()

        viewModelScope.launch {
            userManager
                .getSignInState()
                .asFlow()
                .collect { signInState ->
                    _state.update { it.copy(signInState = signInState) }
                }
        }
    }

    private fun startSyncFlow() {
        syncJob?.cancel()

        syncJob = viewModelScope.launch {
            // Check phone connectivity before starting sync
            if (!phoneConnectionMonitor.isPhoneConnected()) {
                LogBuffer.e(TAG, "Phone not connected - cannot sync")
                _state.update {
                    it.copy(syncState = WatchSyncState.Failed(WatchSyncError.NoPhoneConnection))
                }
                return@launch
            }

            _state.update { it.copy(syncState = WatchSyncState.Syncing) }

            try {
                withTimeout(SYNC_TIMEOUT_MS) {
                    tokenBundleRepository.flow
                        .filterNotNull()
                        .first()
                        .let { watchSyncAuthData ->
                            watchSync.processAuthDataChange(watchSyncAuthData) { result ->
                                onLoginFromPhoneResult(result)
                            }
                        }
                }
            } catch (e: TimeoutCancellationException) {
                LogBuffer.e(TAG, "Watch sync timeout after ${SYNC_TIMEOUT_MS / 1000} seconds")
                _state.update {
                    it.copy(syncState = WatchSyncState.Failed(WatchSyncError.Timeout))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LogBuffer.e(TAG, "Watch sync error: ${e.message}")
                _state.update {
                    it.copy(syncState = WatchSyncState.Failed(WatchSyncError.Unknown(e.message)))
                }
            }
        }
    }

    fun retrySync() {
        val now = System.currentTimeMillis()
        if (now - lastRetryTime < RETRY_DEBOUNCE_MS) {
            LogBuffer.i(TAG, "Retry debounced - too soon after previous attempt")
            return
        }
        lastRetryTime = now
        startSyncFlow()
    }

    private fun onLoginFromPhoneResult(loginResult: LoginResult) {
        when (loginResult) {
            is LoginResult.Failed -> {
                _state.update {
                    it.copy(
                        syncState = WatchSyncState.Failed(
                            WatchSyncError.LoginFailed(loginResult.message),
                        ),
                    )
                }
            }

            is LoginResult.Success -> {
                _state.update {
                    it.copy(
                        syncState = WatchSyncState.Success,
                        showLoggingInScreen = true,
                    )
                }
                viewModelScope.launch {
                    try {
                        podcastManager.refreshPodcastsAfterSignIn()
                    } catch (e: Exception) {
                        LogBuffer.e(TAG, "Failed to refresh podcasts after sign in: ${e.message}")
                    }
                }
            }
        }
        // Cancel the sync job after processing the login result
        syncJob?.cancel()
    }

    fun onSignInConfirmationActionHandled() {
        _state.update {
            it.copy(showLoggingInScreen = false)
        }
    }

    fun signOut() {
        userManager.signOut(playbackManager, wasInitiatedByUser = false)
    }

    override fun onCleared() {
        super.onCleared()
        syncJob?.cancel()
    }

    fun refreshPodcasts() {
        viewModelScope.launch(Dispatchers.IO) {
            delay(REFRESH_START_DELAY) // delay the refresh to allow the UI to load
            try {
                podcastManager.refreshPodcastsIfRequired(fromLog = "watch - open app")
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
        // Schedule next refresh in the background
        RefreshPodcastsTask.scheduleOrCancel(context, settings)
    }

    companion object {
        private const val REFRESH_START_DELAY = 1000L
        private const val SYNC_TIMEOUT_MS = 30_000L
        private const val RETRY_DEBOUNCE_MS = 3_000L
        private const val TAG = "WearMainActivityViewModel"
    }
}
