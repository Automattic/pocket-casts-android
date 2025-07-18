package au.com.shiftyjelly.pocketcasts.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val settings: Settings,
) : ViewModel() {
    internal val uiState = settings.showPlaylistsOnboarding.flow
        .map(::UiState)
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Empty)

    internal data class UiState(
        val showOnboarding: Boolean,
    ) {
        companion object {
            val Empty = UiState(
                showOnboarding = false,
            )
        }
    }
}
