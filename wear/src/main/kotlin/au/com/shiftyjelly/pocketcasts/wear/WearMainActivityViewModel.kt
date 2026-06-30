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
import au.com.shiftyjelly.pocketcasts.wear.networking.ConnectivityStateManager
import au.com.shiftyjelly.pocketcasts.wear.networking.PhoneConnectionMonitor
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.WatchSyncError
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.WatchSyncState
import com.google.android.horologist.auth.data.tokenshare.TokenBundleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import timber.log.Timber

@OptIn(FlowPreview::class)
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
    private val connectivityStateManager: ConnectivityStateManager,
) : ViewModel() {

    data class State(
        val showLoggingInScreen: Boolean = false,
        val signInState: SignInState = SignInState.SignedOut,
        val syncState: WatchSyncState = WatchSyncState.Idle,
        val isConnected: Boolean = true,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private var lastRetryTime: Long = 0L
    private var loginWatchdogJob: Job? = null

    init {
        viewModelScope.launch {
            tokenBundleRepository.flow.collect { watchSyncAuthData ->
                LogBuffer.i(TAG, "Received DataLayer emission: ${if (watchSyncAuthData != null) "non-null" else "null"}")
                if (watchSyncAuthData != null) {
                    _state.update { it.copy(syncState = WatchSyncState.Syncing) }
                }
                watchSync.processAuthDataChange(
                    data = watchSyncAuthData,
                    onResult = { result -> onLoginFromPhoneResult(result) },
                    onAlreadyLoggedIn = {
                        LogBuffer.i(TAG, "Already logged in - treating as sync success")
                        _state.update { it.copy(syncState = WatchSyncState.Success) }
                    },
                )
            }
        }

        viewModelScope.launch {
            phoneConnectionMonitor.isPhoneConnected.collect { connected ->
                _state.update { state ->
                    when {
                        connected == false && state.syncState == WatchSyncState.Idle -> {
                            state.copy(syncState = WatchSyncState.Failed(WatchSyncError.NoPhoneConnection))
                        }

                        connected == true && state.syncState == WatchSyncState.Failed(WatchSyncError.NoPhoneConnection) -> {
                            state.copy(syncState = WatchSyncState.Idle)
                        }

                        else -> state
                    }
                }
            }
        }

        viewModelScope.launch {
            userManager
                .getSignInState()
                .asFlow()
                .collect { signInState ->
                    if (signInState.isSignedInAsPlusOrPatron) {
                        loginWatchdogJob?.cancel()
                    }
                    _state.update { it.copy(signInState = signInState) }
                }
        }

        viewModelScope.launch {
            connectivityStateManager.isConnected
                .debounce(CONNECTIVITY_DEBOUNCE_MS)
                .collect { isConnected ->
                    _state.update { it.copy(isConnected = isConnected) }
                }
        }
    }

    fun restartSyncIfNeeded() {
        if (_state.value.syncState is WatchSyncState.Failed) {
            loginWatchdogJob?.cancel()
            _state.update { it.copy(syncState = resetSyncState()) }
        }
    }

    fun retrySync() {
        val now = System.currentTimeMillis()
        if (now - lastRetryTime < RETRY_DEBOUNCE_MS) {
            LogBuffer.i(TAG, "Retry debounced - too soon after previous attempt")
            return
        }
        lastRetryTime = now
        loginWatchdogJob?.cancel()
        _state.update { it.copy(syncState = resetSyncState()) }
    }

    // The connection collector is edge-triggered, so re-check the current value here to keep
    // showing the warning if the phone is still unreachable after a manual reset.
    private fun resetSyncState(): WatchSyncState {
        return if (phoneConnectionMonitor.isPhoneConnected.value == false) {
            WatchSyncState.Failed(WatchSyncError.NoPhoneConnection)
        } else {
            WatchSyncState.Idle
        }
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
                startLoginWatchdog()
            }
        }
    }

    private fun startLoginWatchdog() {
        loginWatchdogJob?.cancel()
        loginWatchdogJob = viewModelScope.launch {
            delay(LOGIN_RESOLUTION_TIMEOUT_MS)
            val current = _state.value
            if (!current.signInState.isSignedInAsPlusOrPatron && current.syncState == WatchSyncState.Success) {
                LogBuffer.e(TAG, "Login succeeded but subscription was not confirmed in ${LOGIN_RESOLUTION_TIMEOUT_MS / 1000}s; showing retry")
                _state.update {
                    it.copy(
                        syncState = WatchSyncState.Failed(WatchSyncError.LoginFailed(null)),
                        showLoggingInScreen = false,
                    )
                }
            }
        }
    }

    fun onSignInConfirmationActionHandled() {
        _state.update {
            it.copy(showLoggingInScreen = false)
        }
    }

    fun signOut() {
        userManager.signOut(playbackManager, wasInitiatedByUser = false)
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
        private const val RETRY_DEBOUNCE_MS = 3_000L
        private const val CONNECTIVITY_DEBOUNCE_MS = 2_000L
        private const val LOGIN_RESOLUTION_TIMEOUT_MS = 20_000L
        private const val TAG = "WearMainActivityViewModel"
    }
}
