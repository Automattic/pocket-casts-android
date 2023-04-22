package au.com.shiftyjelly.pocketcasts.wear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.watchsync.WatchSync
import au.com.shiftyjelly.pocketcasts.account.watchsync.WatchSyncAuthData
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import com.google.android.horologist.auth.data.tokenshare.TokenBundleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject

@HiltViewModel
class WearMainActivityViewModel @Inject constructor(
    podcastManager: PodcastManager,
    settings: Settings,
    tokenBundleRepository: TokenBundleRepository<WatchSyncAuthData?>,
    watchSync: WatchSync,
) : ViewModel() {

    data class State(
        val signInConfirmationAction: SignInConfirmationAction? = null,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            tokenBundleRepository.flow
                .collect { watchSyncAuthData ->
                    watchSync.processAuthDataChange(watchSyncAuthData) {
                        onLoginResult(it, podcastManager)
                    }
                }
        }

        viewModelScope.launch {
            settings.refreshStateObservable
                .asFlow()
                .collect(::onRefreshStateChange)
        }
    }

    private fun onLoginResult(loginResult: LoginResult, podcastManager: PodcastManager) {
        when (loginResult) {
            is LoginResult.Failed -> { /* do nothing */ }
            is LoginResult.Success -> {
                _state.update {
                    it.copy(signInConfirmationAction = SignInConfirmationAction.Show(loginResult.result.email))
                }
                viewModelScope.launch {
                    podcastManager.refreshPodcastsAfterSignIn()
                }
            }
        }
    }

    private fun onRefreshStateChange(refreshState: RefreshState) {
        when (refreshState) {
            RefreshState.Never,
            RefreshState.Refreshing -> { /* Do nothing */ }

            is RefreshState.Failed,
            is RefreshState.Success -> {
                _state.update { it.copy(signInConfirmationAction = SignInConfirmationAction.Hide) }
            }
        }
    }

    /**
     * This should be invoked when the UI it has handled showing or hiding the sign in
     * confirmation.
     */
    fun onSignInConfirmationActionHandled() {
        _state.update { it.copy(signInConfirmationAction = null) }
    }
}

sealed class SignInConfirmationAction {
    class Show(val email: String) : SignInConfirmationAction()
    object Hide : SignInConfirmationAction()
}
