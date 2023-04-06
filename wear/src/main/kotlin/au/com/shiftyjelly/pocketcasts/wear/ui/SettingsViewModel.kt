package au.com.shiftyjelly.pocketcasts.wear.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackManager: PlaybackManager,
    private val userManager: UserManager,
    private val settings: Settings,
) : ViewModel() {

    data class State(
        val signInState: SignInState,
        val showDataWarning: Boolean,
    )

    private val _state = MutableStateFlow(
        State(
            signInState = userManager.getSignInState().blockingFirst(),
            showDataWarning = settings.warnOnMeteredNetwork(),
        )
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            userManager.getSignInState()
                .asFlow()
                .collectLatest { signInState ->
                    _state.update { it.copy(signInState = signInState) }
                }
        }
    }

    fun setWarnOnMeteredNetwork(warnOnMeteredNetwork: Boolean) {
        settings.setWarnOnMeteredNetwork(warnOnMeteredNetwork)
        _state.update { it.copy(showDataWarning = warnOnMeteredNetwork) }
    }

    fun signOut() {
        userManager.signOut(
            playbackManager = playbackManager,
            wasInitiatedByUser = true
        )
    }
}
