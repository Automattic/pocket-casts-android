package au.com.shiftyjelly.pocketcasts.wear

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.watchsync.WatchSync
import au.com.shiftyjelly.pocketcasts.account.watchsync.WatchSyncAuthData
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsTask
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.android.horologist.auth.data.tokenshare.TokenBundleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    private val settings: Settings,
    private val syncManager: SyncManager,
    watchSync: WatchSync,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    data class State(
        val email: String?,
        val showLoggingInScreen: Boolean = false,
        val signInState: SignInState? = null,
        val subscriptionStatus: SubscriptionStatus? = null,
    )

    private val _state = MutableStateFlow(State(email = syncManager.getEmail()))
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
                    _state.update {
                        it.copy(
                            email = syncManager.getEmail(),
                            subscriptionStatus = subscriptionStatus.get(),
                        )
                    }
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
                    it.copy(
                        email = syncManager.getEmail(),
                        showLoggingInScreen = true,
                    )
                }
            }
        }
    }

    /**
     * This should be invoked when the UI has handled showing or hiding the sign in confirmation.
     */
    fun onSignInConfirmationActionHandled() {
        _state.update {
            it.copy(
                email = syncManager.getEmail(),
                showLoggingInScreen = false,
            )
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
                LogBuffer.logException(LogBuffer.TAG_BACKGROUND_TASKS, e, "Failed to start refresh")
            }
        }
        // Schedule next refresh in the background
        RefreshPodcastsTask.scheduleOrCancel(context, settings)
    }

    companion object {
        private const val REFRESH_START_DELAY = 1000L
    }
}
