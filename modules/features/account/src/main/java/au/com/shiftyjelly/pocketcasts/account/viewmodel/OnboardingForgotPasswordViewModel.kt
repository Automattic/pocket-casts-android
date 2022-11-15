package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.account.AccountAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class OnboardingForgotPasswordViewModel @Inject constructor(
    private val auth: AccountAuth
) : ViewModel() {

    private val _stateFlow = MutableStateFlow(
        OnboardingForgotPasswordState()
    )
    val stateFlow: StateFlow<OnboardingForgotPasswordState> = _stateFlow

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

        auth.resetPasswordWithEmail(emailString) { result ->
            when (result) {
                is AccountAuth.AuthResult.Success -> { onCompleted() }
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

data class OnboardingForgotPasswordState(
    val email: String = "",
    private val hasAttemptedReset: Boolean = false,
    private val isCallInProgress: Boolean = false,
    val serverErrorMessage: String? = null,
) {
    val enableSubmissionFields = !isCallInProgress
}
