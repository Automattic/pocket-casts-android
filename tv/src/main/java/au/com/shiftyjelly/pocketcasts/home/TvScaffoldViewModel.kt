package au.com.shiftyjelly.pocketcasts.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.reactive.asFlow

@HiltViewModel
class TvScaffoldViewModel @Inject constructor(
    private val userManager: UserManager,
    private val playbackManager: PlaybackManager,
) : ViewModel() {
    private val selectedTabIndex = MutableStateFlow(0)

    val uiState: StateFlow<TvScaffoldUiState> = combine(
        selectedTabIndex,
        userManager.getSignInState().asFlow(),
    ) { tabIndex, signInState ->
        TvScaffoldUiState(
            selectedTabIndex = tabIndex,
            profile = signInState.toProfileState(),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TvScaffoldUiState())

    fun selectTab(index: Int) {
        selectedTabIndex.value = index
    }

    fun signOut() {
        userManager.signOut(playbackManager, wasInitiatedByUser = true)
    }

    private fun SignInState.toProfileState() = when (this) {
        is SignInState.SignedIn -> TvProfileState.SignedIn(email = email)
        is SignInState.SignedOut -> TvProfileState.SignedOut
    }
}

data class TvScaffoldUiState(
    val tabs: List<TvTab> = TvTab.entries,
    val selectedTabIndex: Int = 0,
    val profile: TvProfileState = TvProfileState.SignedOut,
)

sealed interface TvProfileState {
    data object SignedOut : TvProfileState
    data class SignedIn(val email: String) : TvProfileState
}
