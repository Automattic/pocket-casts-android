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
import com.google.android.horologist.auth.data.tokenshare.TokenBundleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
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
) : ViewModel() {

    data class State(
        val showLoggingInScreen: Boolean = false,
        val signInState: SignInState = SignInState.SignedOut,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            tokenBundleRepository.flow
                .collect { watchSyncAuthData ->
                    watchSync.processAuthDataChange(watchSyncAuthData) {
                        onLoginFromPhoneResult(it)
                    }
                }
        }

        viewModelScope.launch {
            userManager
                .getSignInState()
                .asFlow()
                .collect { signInState ->
                    _state.update { it.copy(signInState = signInState) }
                }
        }
    }

    private fun onLoginFromPhoneResult(loginResult: LoginResult) {
        when (loginResult) {
            is LoginResult.Failed -> { /* do nothing */ }
            is LoginResult.Success -> {
                viewModelScope.launch {
                    podcastManager.refreshPodcastsAfterSignIn()
                }
                _state.update {
                    it.copy(showLoggingInScreen = true)
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
    }
}
