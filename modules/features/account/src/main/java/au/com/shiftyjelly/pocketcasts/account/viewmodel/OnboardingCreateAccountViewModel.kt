package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.AccountAuth
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
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
    private val auth: AccountAuth,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val subscriptionManager: SubscriptionManager,
    private val settings: Settings,
) : ViewModel(), CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val _stateFlow = MutableStateFlow(
        OnboardingCreateAccountState()
    )
    val stateFlow: StateFlow<OnboardingCreateAccountState> = _stateFlow

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

            val result = auth.createUserWithEmailAndPassword(state.email, state.password)
            when (result) {

                is AccountAuth.AuthResult.Success -> {
                    analyticsTracker.refreshMetadata()
                    onAccountCreated()
                }

                is AccountAuth.AuthResult.Failed -> {
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
