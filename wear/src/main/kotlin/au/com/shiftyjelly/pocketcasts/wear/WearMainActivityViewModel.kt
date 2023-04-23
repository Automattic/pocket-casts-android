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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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

    // The time that the most recent login notification was shown.
    private var logInNotificationShownMs: Long? = null
    private val logInNotificationMinDuration = 5.seconds

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
                logInNotificationShownMs = System.currentTimeMillis()
                _state.update {
                    it.copy(signInConfirmationAction = SignInConfirmationAction.Show)
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
                viewModelScope.launch {
                    delayHidingLoginNotification(refreshState)
                    _state.update { it.copy(signInConfirmationAction = SignInConfirmationAction.Hide) }
                }
            }
        }
    }

    private suspend fun delayHidingLoginNotification(refreshState: RefreshState) {
        if (logInNotificationShownMs == null) {
            Timber.e("logInNotificationShownMs was null when refresh state changed to $refreshState. This should never happen")
        }

        val notificationDuration = logInNotificationShownMs?.let {
            (System.currentTimeMillis() - it).milliseconds
        } ?: 0.milliseconds

        if (notificationDuration < logInNotificationMinDuration) {
            val delayAmount = logInNotificationMinDuration - notificationDuration
            delay(delayAmount)
        }
        logInNotificationShownMs = null
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
    object Show : SignInConfirmationAction()
    object Hide : SignInConfirmationAction()
}
