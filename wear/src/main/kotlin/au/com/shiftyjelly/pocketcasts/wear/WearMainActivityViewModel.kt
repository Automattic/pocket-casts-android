package au.com.shiftyjelly.pocketcasts.wear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.watchsync.WatchSync
import au.com.shiftyjelly.pocketcasts.account.watchsync.WatchSyncAuthData
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import com.google.android.horologist.auth.data.tokenshare.TokenBundleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WearMainActivityViewModel @Inject constructor(
    tokenBundleRepository: TokenBundleRepository<WatchSyncAuthData?>,
    watchSync: WatchSync,
) : ViewModel() {

    data class State(
        val signInConfirmationAction: SignInConfirmationAction? = null,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            tokenBundleRepository.flow
                .collect { watchSyncAuthData ->
                    watchSync.processAuthDataChange(watchSyncAuthData) {
                        onLoginResult(it)
                    }
                }
        }
    }

    private fun onLoginResult(loginResult: LoginResult) {
        when (loginResult) {
            is LoginResult.Failed -> { /* do nothing */ }
            is LoginResult.Success -> {
                _state.update {
                    it.copy(signInConfirmationAction = SignInConfirmationAction.Show)
                }
            }
        }
    }

    /**
     * This should be invoked when the UI has handled showing or hiding the sign in confirmation.
     */
    fun onSignInConfirmationActionHandled() {
        _state.update { it.copy(signInConfirmationAction = null) }
    }
}

sealed class SignInConfirmationAction {
    object Show : SignInConfirmationAction()
    object Hide : SignInConfirmationAction()
}
