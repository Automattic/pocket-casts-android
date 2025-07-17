package au.com.shiftyjelly.pocketcasts.wear.ui.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SmartPlaylistManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class FiltersViewModel @Inject constructor(
    smartPlaylistManager: SmartPlaylistManager,
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Loaded(val filters: List<SmartPlaylist>) : UiState()
    }

    val uiState = smartPlaylistManager.findAllFlow()
        .map { UiState.Loaded(filters = it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, UiState.Loading)
}
