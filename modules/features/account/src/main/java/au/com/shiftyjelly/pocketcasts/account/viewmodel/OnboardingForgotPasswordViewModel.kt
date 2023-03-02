package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.servers.account.SyncAccountManager
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingForgotPasswordViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val syncAccountManager: SyncAccountManager,
    private val syncServerManager: SyncServerManager
) : ViewModel() {

    private val _stateFlow = MutableStateFlow(
        OnboardingForgotPasswordState()
    )
    val stateFlow: StateFlow<OnboardingForgotPasswordState> = _stateFlow

    fun onShown() {
        analyticsTracker.track(AnalyticsEvent.FORGOT_PASSWORD_SHOWN)
    }

    fun onBackPressed() {
        analyticsTracker.track(AnalyticsEvent.FORGOT_PASSWORD_DISMISSED)
    }

    fun updateEmail(email: String) {
        _stateFlow.update { it.copy(email = email.trim()) }
    }

    fun resetPassword(onCompleted: () -> Unit) {
        _stateFlow.update { it.copy(hasAttemptedReset = true) }

        val emailString = stateFlow.value.email
        if (emailString.isEmpty()) {
            return
        }

        _stateFlow.update {
            it.copy(
                isCallInProgress = true,
                serverErrorMessage = null,
            )
        }

        viewModelScope.launch {
            syncAccountManager.forgotPassword(
                email = emailString,
                syncServerManager = syncServerManager,
                onSuccess = onCompleted,
                onError = { message ->
                    _stateFlow.update {
                        it.copy(
                            isCallInProgress = false,
                            serverErrorMessage = message
                        )
                    }
                }
            )
        }
    }
}

data class OnboardingForgotPasswordState(
    val email: String = "",
    private val hasAttemptedReset: Boolean = false,
    private val isCallInProgress: Boolean = false,
    val serverErrorMessage: String? = null,
) {
    val enableSubmissionFields = !isCallInProgress
}
