package au.com.shiftyjelly.pocketcasts.onboarding.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.sync.SignInSource
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class TvSignInViewModel @Inject constructor(
    private val syncManager: SyncManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TvSignInUiState>(TvSignInUiState.Loading)
    val uiState: StateFlow<TvSignInUiState> = _uiState.asStateFlow()

    init {
        requestDeviceCode()
    }

    private fun requestDeviceCode() {
        viewModelScope.launch {
            _uiState.value = TvSignInUiState.Loading
            try {
                val response = syncManager.deviceAuthorize()
                _uiState.value = TvSignInUiState.Ready(
                    userCode = response.userCode.map { it.toString() },
                    verificationUri = response.verificationUri,
                    verificationUriComplete = response.verificationUriComplete,
                )
                pollForApproval(response.deviceCode, response.interval.toLong())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to request device code")
                _uiState.value = TvSignInUiState.Error
            }
        }
    }

    private suspend fun pollForApproval(deviceCode: String, intervalSeconds: Long) {
        while (true) {
            delay(intervalSeconds.coerceAtLeast(MIN_POLL_INTERVAL_SECONDS) * 1000)
            val result = syncManager.loginWithDeviceAuth(
                deviceCode = deviceCode,
                signInSource = SignInSource.UserInitiated.Onboarding,
            )
            when {
                result is LoginResult.Success -> {
                    _uiState.value = TvSignInUiState.Complete
                    return
                }

                result is LoginResult.Failed && result.messageId == AUTHORIZATION_PENDING -> {
                    // Keep polling
                }

                else -> {
                    Timber.w("Device auth polling stopped: ${(result as? LoginResult.Failed)?.message}")
                    _uiState.value = TvSignInUiState.Error
                    return
                }
            }
        }
    }

    fun retry() {
        requestDeviceCode()
    }

    companion object {
        private const val AUTHORIZATION_PENDING = "authorization_pending"
        private const val MIN_POLL_INTERVAL_SECONDS = 5L
    }
}

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
