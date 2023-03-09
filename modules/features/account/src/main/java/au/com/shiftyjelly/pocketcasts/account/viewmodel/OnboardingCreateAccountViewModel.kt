package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.servers.account.LoginResult
import au.com.shiftyjelly.pocketcasts.servers.account.SyncAccountManager
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class OnboardingCreateAccountViewModel @Inject constructor(
    private val syncAccountManager: SyncAccountManager,
    private val syncServerManager: SyncServerManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val subscriptionManager: SubscriptionManager,
    private val podcastManager: PodcastManager
) : ViewModel(), CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val _stateFlow = MutableStateFlow(
        OnboardingCreateAccountState()
    )
    val stateFlow: StateFlow<OnboardingCreateAccountState> = _stateFlow

    fun onShown() {
        analyticsTracker.track(AnalyticsEvent.CREATE_ACCOUNT_SHOWN)
    }

    fun onBackPressed() {
        analyticsTracker.track(AnalyticsEvent.CREATE_ACCOUNT_DISMISSED)
    }

    fun updateEmail(email: String) {
        _stateFlow.update { it.copy(email = email.trim()) }
    }

    fun updatePassword(password: String) {
        _stateFlow.update { it.copy(password = password) }
    }

    fun createAccount(onAccountCreated: () -> Unit) {
        _stateFlow.update { it.copy(hasAttemptedLogIn = true) }

        val state = stateFlow.value
        if (!state.isEmailValid || !state.isPasswordValid) {
            return
        }

        _stateFlow.update {
            it.copy(
                isCallInProgress = true,
                serverErrorMessage = null,
            )
        }

        subscriptionManager.clearCachedStatus()

        viewModelScope.launch {
            val result = syncAccountManager.createUserWithEmailAndPassword(
                email = state.email,
                password = state.password,
                syncServerManager = syncServerManager
            )
            when (result) {
                is LoginResult.Success -> {
                    podcastManager.refreshPodcastsAfterSignIn()
                    analyticsTracker.refreshMetadata()
                    onAccountCreated()
                }

                is LoginResult.Failed -> {
                    _stateFlow.update {
                        it.copy(
                            isCallInProgress = false,
                            serverErrorMessage = result.message
                        )
                    }
                }
            }
        }
    }
}

data class OnboardingCreateAccountState(
    val email: String = "",
    private val hasAttemptedLogIn: Boolean = false,
    private val isCallInProgress: Boolean = false,
    val password: String = "",
    val serverErrorMessage: String? = null,
) {
    val isEmailValid = AccountViewModel.isEmailValid(email)
    val isPasswordValid = AccountViewModel.isPasswordValid(password)

    val showEmailError = hasAttemptedLogIn && !isEmailValid
    val showPasswordError = hasAttemptedLogIn && !isPasswordValid

    val enableSubmissionFields = !isCallInProgress
}
