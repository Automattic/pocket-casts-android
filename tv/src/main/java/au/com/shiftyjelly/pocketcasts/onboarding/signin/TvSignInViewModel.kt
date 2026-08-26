package au.com.shiftyjelly.pocketcasts.onboarding.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.sync.SignInSource
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SignInType
import com.automattic.eventhorizon.SignInTypeTappedEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TvSignInViewModel @Inject constructor(
    private val syncManager: SyncManager,
    private val eventHorizon: EventHorizon,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TvSignInUiState>(TvSignInUiState.Loading)
    val uiState: StateFlow<TvSignInUiState> = _uiState.asStateFlow()

    private val _mode = MutableStateFlow(TvSignInMode.QrCode)
    val mode: StateFlow<TvSignInMode> = _mode.asStateFlow()

    private val _emailState = MutableStateFlow(TvEmailSignInState())
    val emailState: StateFlow<TvEmailSignInState> = _emailState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        requestDeviceCode()
    }

    fun selectMode(mode: TvSignInMode) {
        if (_mode.value == mode) return
        _mode.value = mode
        eventHorizon.track(SignInTypeTappedEvent(type = mode.analyticsType))
        if (mode == TvSignInMode.Email) {
            _emailState.update { it.copy(showEmailError = false, showPasswordError = false, serverError = null) }
        }
    }

    fun updateEmail(email: String) {
        _emailState.update { it.copy(email = email, showEmailError = false, serverError = null) }
    }

    fun updatePassword(password: String) {
        _emailState.update { it.copy(password = password, showPasswordError = false, serverError = null) }
    }

    fun submitEmailSignIn() {
        val current = _emailState.value
        if (current.isSubmitting) return

        val emailValid = isEmailValid(current.email)
        val passwordValid = isPasswordValid(current.password)
        if (!emailValid || !passwordValid) {
            _emailState.update { it.copy(showEmailError = !emailValid, showPasswordError = !passwordValid) }
            return
        }

        _emailState.update {
            it.copy(isSubmitting = true, showEmailError = false, showPasswordError = false, serverError = null)
        }
        viewModelScope.launch {
            val result = syncManager.loginWithEmailAndPassword(
                email = current.email,
                password = current.password,
                signInSource = SignInSource.UserInitiated.Onboarding,
            )
            when (result) {
                is LoginResult.Success -> _uiState.value = TvSignInUiState.Complete

                is LoginResult.Failed -> _emailState.update {
                    it.copy(isSubmitting = false, serverError = result.message)
                }
            }
        }
    }

    fun retry() {
        requestDeviceCode()
    }

    private fun requestDeviceCode() {
        pollingJob?.cancel()
        _uiState.value = TvSignInUiState.Loading
        pollingJob = viewModelScope.launch {
            deviceAuthFlow(syncManager).collect { _uiState.value = it }
        }
    }

    private fun isEmailValid(email: String) = EMAIL_REGEX.matches(email)

    private fun isPasswordValid(password: String) = password.length >= MIN_PASSWORD_LENGTH

    private companion object {
        const val MIN_PASSWORD_LENGTH = 6
        val EMAIL_REGEX = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    }
}

enum class TvSignInMode(val analyticsType: SignInType) {
    QrCode(SignInType.Qr),
    Email(SignInType.Password),
}

data class TvEmailSignInState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val showEmailError: Boolean = false,
    val showPasswordError: Boolean = false,
    val serverError: String? = null,
)

sealed interface TvSignInUiState {
    data object Loading : TvSignInUiState
    data class Ready(
        val userCode: List<String>,
        val verificationUri: String,
        val verificationUriComplete: String,
    ) : TvSignInUiState
    data object Error : TvSignInUiState
    data object Complete : TvSignInUiState
}
