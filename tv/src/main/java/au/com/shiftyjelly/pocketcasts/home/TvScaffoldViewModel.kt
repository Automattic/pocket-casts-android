package au.com.shiftyjelly.pocketcasts.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.reactive.asFlow

@HiltViewModel
class TvScaffoldViewModel @Inject constructor(
    private val userManager: UserManager,
    private val syncManager: SyncManager,
    private val playbackManager: Lazy<PlaybackManager>,
) : ViewModel() {
    private val selectedTabIndex = MutableStateFlow(0)

    private val initialProfile = currentProfileState()

    val uiState: StateFlow<TvScaffoldUiState> = combine(
        selectedTabIndex,
        userManager.getSignInState().asFlow()
            .map { it.toProfileState() }
            .onStart { emit(initialProfile) },
    ) { tabIndex, profile ->
        TvScaffoldUiState(
            selectedTabIndex = tabIndex,
            profile = profile,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TvScaffoldUiState(profile = initialProfile),
    )

    fun selectTab(index: Int) {
        selectedTabIndex.value = index
    }

    fun signOut() {
        userManager.signOut(playbackManager.get(), wasInitiatedByUser = true)
    }

    private fun currentProfileState() = if (syncManager.isLoggedIn()) {
        TvProfileState.SignedIn(email = syncManager.getEmail()?.takeIf(String::isNotBlank))
    } else {
        TvProfileState.SignedOut
    }

    private fun SignInState.toProfileState() = when (this) {
        is SignInState.SignedIn -> TvProfileState.SignedIn(email = email.takeIf(String::isNotBlank))
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
    data class SignedIn(val email: String?) : TvProfileState
}
