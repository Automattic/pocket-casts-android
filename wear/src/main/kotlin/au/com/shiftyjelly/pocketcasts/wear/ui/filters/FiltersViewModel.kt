package au.com.shiftyjelly.pocketcasts.wear.ui.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FiltersViewModel @Inject constructor(
    playlistManager: PlaylistManager
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Loaded(val filters: List<Playlist>) : UiState()
    }

    val uiState = playlistManager.findAllFlow()
        .map { UiState.Loaded(filters = it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, UiState.Loading)
}
