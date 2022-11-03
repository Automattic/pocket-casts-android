package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.AccountAuth
import au.com.shiftyjelly.pocketcasts.account.SignInSource
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
class OnboardingLogInViewModel @Inject constructor(
    private val auth: AccountAuth,
    private val subscriptionManager: SubscriptionManager,
) : ViewModel(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val _logInState = MutableStateFlow(
        LogInState(logIn = ::logIn)
    )
    val logInState: StateFlow<LogInState> = _logInState

    fun updateEmail(email: String) {
        _logInState.update { it.copy(email = email.trim()) }
    }

    fun updatePassword(password: String) {
        _logInState.update { it.copy(password = password) }
    }

    fun logIn() {
        _logInState.update { it.copy(hasAttemptedLogIn = true) }

        val state = logInState.value
        if (!state.isEmailValid || !state.isPasswordValid) {
            return
        }

        val email = state.email
        val password = state.password

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
    val serverErrorMessage: String? = null,
    val callState: CallState = CallState.None,
    private var hasAttemptedLogIn: Boolean = false,
    private val logIn: () -> Unit,
) {
    val isEmailValid = AccountViewModel.isEmailValid(email)
    val isPasswordValid = AccountViewModel.isPasswordValid(password)

    val showEmailError = hasAttemptedLogIn && !isEmailValid
    val showPasswordError = hasAttemptedLogIn && !isPasswordValid

    val enableLogin =
        isEmailValid &&
            isPasswordValid &&
            callState != CallState.InProgress

    val enableTextFields = callState != CallState.InProgress

    enum class CallState {
        None,
        InProgress,
        Successful
    }
}
