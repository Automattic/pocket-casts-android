package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.AccountAuth
import au.com.shiftyjelly.pocketcasts.account.SignInSource
import au.com.shiftyjelly.pocketcasts.account.viewmodel.AccountViewModel
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

// This is largely based on and, eventually, should supersede the SignInViewModel
@HiltViewModel
class LogInViewModel @Inject constructor(
    private val auth: AccountAuth,
    private val subscriptionManager: SubscriptionManager,
) : ViewModel(), CoroutineScope {

    private val _logInState = MutableStateFlow(LogInState())
    val logInState: StateFlow<LogInState> = _logInState

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    fun updateEmail(value: String) {
        val email = value.trim()
        _logInState.update {
            it.copy(
                email = email,
                isEmailValid = AccountViewModel.isEmailValid(email)
            )
        }
    }

    fun updatePassword(value: String) {
        _logInState.update {
            it.copy(
                password = value,
                isPasswordValid = AccountViewModel.isPasswordValid(value)
            )
        }
    }

    fun signIn() {
        val email = logInState.value.email
        val password = logInState.value.password

        if (email.isBlank() || password.isBlank()) {
            return
        }
        _logInState.update {
            it.copy(
                callState = LogInState.CallState.InProgress,
                serverErrorMessage = null,
            )
        }

        subscriptionManager.clearCachedStatus()
        viewModelScope.launch {
            val result = auth.signInWithEmailAndPassword(email, password, SignInSource.Onboarding)
            when (result) {
                is AccountAuth.AuthResult.Success -> {
                    _logInState.update { it.copy(callState = LogInState.CallState.Successful) }
                }

                is AccountAuth.AuthResult.Failed -> {
                    _logInState.update {
                        it.copy(
                            callState = LogInState.CallState.None,
                            serverErrorMessage = result.message,
                        )
                    }
                }
            }
        }
    }
}

data class LogInState(
    val email: String = "",
    val password: String = "",
    val newsletter: Boolean = false,
    val isPasswordValid: Boolean = false,
    val isEmailValid: Boolean = false,
    val serverErrorMessage: String? = null,
    val callState: CallState = CallState.None
) {
    val enableLoginButton = isEmailValid && isPasswordValid && callState != CallState.InProgress
    val enableTextFields = callState != CallState.InProgress

    enum class CallState {
        None,
        InProgress,
        Successful
    }
}
