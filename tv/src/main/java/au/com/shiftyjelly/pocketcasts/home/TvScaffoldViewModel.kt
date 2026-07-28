package au.com.shiftyjelly.pocketcasts.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.auth.TvSignOutManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.rx2.asFlow

@HiltViewModel
class TvScaffoldViewModel @Inject constructor(
    private val userManager: UserManager,
    private val syncManager: SyncManager,
    private val signOutManager: TvSignOutManager,
) : ViewModel() {
    private val selectedTabIndex = MutableStateFlow(0)

    val uiState: StateFlow<TvScaffoldUiState> = combine(
        selectedTabIndex,
        syncManager.isLoggedInObservable.asFlow(),
        syncManager.emailFlow(),
    ) { tabIndex, isLoggedIn, email ->
        TvScaffoldUiState(
            selectedTabIndex = tabIndex,
            profile = profileState(isLoggedIn, email),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TvScaffoldUiState(profile = currentProfileState()),
    )

    fun selectTab(index: Int) {
        selectedTabIndex.value = index
    }

    fun signOut() {
        signOutManager.signOutAndWipeData()
    }

    private fun currentProfileState() = profileState(syncManager.isLoggedIn(), syncManager.getEmail())

    private fun profileState(isLoggedIn: Boolean, email: String?) = if (isLoggedIn) {
        TvProfileState.SignedIn(email = email?.takeIf(String::isNotBlank))
    } else {
        TvProfileState.SignedOut
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
