package au.com.shiftyjelly.pocketcasts.wear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.watchsync.WatchSync
import au.com.shiftyjelly.pocketcasts.account.watchsync.WatchSyncAuthData
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import com.google.android.horologist.auth.data.tokenshare.TokenBundleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import javax.inject.Inject

@HiltViewModel
class WearMainActivityViewModel @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val podcastManager: PodcastManager,
    tokenBundleRepository: TokenBundleRepository<WatchSyncAuthData?>,
    subscriptionManager: SubscriptionManager,
    private val userManager: UserManager,
    watchSync: WatchSync,
) : ViewModel() {

    data class State(
        val signInConfirmationAction: SignInConfirmationAction? = null,
        val signInState: SignInState? = null,
        val subscriptionStatus: SubscriptionStatus? = null,
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

        viewModelScope.launch {
            subscriptionManager
                .observeSubscriptionStatus()
                .asFlow()
                .collect { subscriptionStatus ->
                    _state.update { it.copy(subscriptionStatus = subscriptionStatus.get()) }
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
                    it.copy(signInConfirmationAction = SignInConfirmationAction.Show)
                }
            }
        }
    }

    /**
     * This should be invoked when the UI has handled showing or hiding the sign in confirmation.
     */
    fun onSignInConfirmationActionHandled() {
        _state.update { it.copy(signInConfirmationAction = null) }
    }

    fun signOut() {
        userManager.signOut(playbackManager, wasInitiatedByUser = false)
    }
}

sealed class SignInConfirmationAction {
    object Show : SignInConfirmationAction()
    object Hide : SignInConfirmationAction()
}
