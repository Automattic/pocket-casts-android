package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.AccountAuth
import au.com.shiftyjelly.pocketcasts.account.SignInSource
import au.com.shiftyjelly.pocketcasts.account.viewmodel.AccountViewModel
import au.com.shiftyjelly.pocketcasts.compose.components.EmailPasswordFieldsState
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

    private val _logInState = MutableStateFlow(
        LogInState(
            signIn = ::signIn,
            onUpdateEmail = ::updateEmail,
            onUpdatePassword = ::updatePassword,
        )
    )
    val logInState: StateFlow<LogInState> = _logInState

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    fun updateEmail(value: String) {
        _logInState.update { it.copy(email = value.trim()) }
    }

    fun updatePassword(value: String) {
        _logInState.update { it.copy(password = value) }
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
                errorMessage = null,
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
                            errorMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    data class LogInState(
        val email: String = "",
        val password: String = "",
        val errorMessage: String? = null,
        val callState: CallState = CallState.None,
        private val signIn: () -> Unit,
        private val onUpdateEmail: (String) -> Unit,
        private val onUpdatePassword: (String) -> Unit
    ) {
        val enableLoginButton = AccountViewModel.isEmailValid(email) &&
            AccountViewModel.isPasswordValid(password) &&
            callState != CallState.InProgress

        val emailPasswordState = EmailPasswordFieldsState(
            email = email,
            password = password,
            enabled = callState != CallState.InProgress,
            hasError = errorMessage != null,
            onPasswordDone = signIn,
            onUpdateEmail = onUpdateEmail,
            onUpdatePassword = onUpdatePassword,
        )

        enum class CallState {
            None,
            InProgress,
            Successful
        }
    }
}
