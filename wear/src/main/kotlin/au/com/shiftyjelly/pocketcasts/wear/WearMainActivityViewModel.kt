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
    private val tokenBundleRepository: TokenBundleRepository<WatchSyncAuthData?>,
    private val watchSync: WatchSync,
) : ViewModel() {

    data class State(
        val showSignInConfirmation: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            tokenBundleRepository.flow
                .collect { watchSyncAuthData ->
                    watchSync.processAuthDataChange(watchSyncAuthData) { loginResult ->
                        when (loginResult) {
                            is LoginResult.Failed -> { /* do nothing */ }
                            is LoginResult.Success -> {
                                _state.update { it.copy(showSignInConfirmation = true) }
                            }
                        }
                    }
                }
        }
    }

    /**
     * This should be invoked by the UI when it shows the sign in confirmation.
     */
    fun onSignInConfirmationShown() {
        _state.update { it.copy(showSignInConfirmation = false) }
    }
}
