package au.com.shiftyjelly.pocketcasts.wear.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val podcastManager: PodcastManager,
    private val userManager: UserManager,
    private val settings: Settings,
) : ViewModel() {

    data class State(
        val refreshState: RefreshState?,
        val signInState: SignInState,
        val showDataWarning: Boolean,
        val refreshInBackground: Boolean,
    )

    private val _state = MutableStateFlow(
        State(
            refreshState = null,
            signInState = userManager.getSignInState().blockingFirst(),
            showDataWarning = settings.warnOnMeteredNetwork.flow.value,
            refreshInBackground = settings.backgroundRefreshPodcasts.flow.value,
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

        viewModelScope.launch {
            settings.refreshStateObservable
                .asFlow()
                .collectLatest { refreshState ->
                    _state.update { it.copy(refreshState = refreshState) }
                }
        }
    }

    fun setWarnOnMeteredNetwork(warnOnMeteredNetwork: Boolean) {
        settings.warnOnMeteredNetwork.set(warnOnMeteredNetwork)
        _state.update { it.copy(showDataWarning = warnOnMeteredNetwork) }
    }

    fun setRefreshPodcastsInBackground(isChecked: Boolean) {
        settings.backgroundRefreshPodcasts.set(isChecked)
        _state.update { it.copy(refreshInBackground = isChecked) }
    }

    fun signOut() {
        userManager.signOut(
            playbackManager = playbackManager,
            wasInitiatedByUser = true
        )
    }

    fun refresh() {
        podcastManager.refreshPodcasts("watch - settings")
    }
}
