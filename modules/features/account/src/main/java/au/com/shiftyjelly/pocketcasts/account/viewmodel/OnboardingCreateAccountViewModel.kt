package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.utils.Network
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val syncManager: SyncManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val subscriptionManager: SubscriptionManager,
    private val podcastManager: PodcastManager,
    @ApplicationContext context: Context
) : AndroidViewModel(context as Application), CoroutineScope {
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
        _stateFlow.update { it.copy(hasAttemptedLogIn = true, isNetworkAvailable = Network.isConnected(getApplication())) }

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
            val result = syncManager.createUserWithEmailAndPassword(
                email = state.email,
                password = state.password,
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
    val password: String = "",
    val serverErrorMessage: String? = null,
    private val hasAttemptedLogIn: Boolean = false,
    private val isCallInProgress: Boolean = false,
    private val isNetworkAvailable: Boolean = true
) {
    val isEmailValid = AccountViewModel.isEmailValid(email)
    val isPasswordValid = AccountViewModel.isPasswordValid(password)

    val showEmailError = hasAttemptedLogIn && !isEmailValid
    val showPasswordError = hasAttemptedLogIn && !isPasswordValid
    val showNetworkError = hasAttemptedLogIn && !isNetworkAvailable

    val enableSubmissionFields = !isCallInProgress
}
