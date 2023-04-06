package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.sync.SignInSource
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
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
class OnboardingLogInViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val podcastManager: PodcastManager,
    private val subscriptionManager: SubscriptionManager,
    private val syncManager: SyncManager,
) : ViewModel(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val _state = MutableStateFlow(LogInState())
    val state: StateFlow<LogInState> = _state

    fun updateEmail(email: String) {
        _state.update { it.copy(email = email.trim()) }
    }

    fun updatePassword(password: String) {
        _state.update { it.copy(password = password) }
    }

    fun logIn(onSuccessfulLogin: () -> Unit) {
        _state.update { it.copy(hasAttemptedLogIn = true) }

        val state = state.value
        if (!state.isEmailValid || !state.isPasswordValid) {
            return
        }

        _state.update {
            it.copy(
                isCallInProgress = true,
                serverErrorMessage = null,
            )
        }

        subscriptionManager.clearCachedStatus()

        viewModelScope.launch {
            val result = syncManager.loginWithEmailAndPassword(
                email = state.email,
                password = state.password,
                signInSource = SignInSource.Onboarding
            )
            when (result) {
                is LoginResult.Success -> {
                    podcastManager.refreshPodcastsAfterSignIn()
                    onSuccessfulLogin()
                }

                is LoginResult.Failed -> {
                    _state.update {
                        it.copy(
                            isCallInProgress = false,
                            serverErrorMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    fun onShown() {
        analyticsTracker.track(AnalyticsEvent.SIGNIN_SHOWN)
    }

    fun onBackPressed() {
        analyticsTracker.track(AnalyticsEvent.SIGNIN_DISMISSED)
    }
}

data class LogInState(
    val email: String = "",
    val password: String = "",
    val serverErrorMessage: String? = null,
    private val isCallInProgress: Boolean = false,
    private val hasAttemptedLogIn: Boolean = false,
) {
    val isEmailValid = AccountViewModel.isEmailValid(email)
    val isPasswordValid = AccountViewModel.isPasswordValid(password)

    val showEmailError = hasAttemptedLogIn && !isEmailValid
    val showPasswordError = hasAttemptedLogIn && !isPasswordValid

    val enableSubmissionFields = !isCallInProgress
}
