package au.com.shiftyjelly.pocketcasts.playlists.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.compose.text.SearchFieldState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playlist.ManualPlaylist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = PlaylistViewModel.Factory::class)
class PlaylistViewModel @AssistedInject constructor(
    @Assisted private val playlistUuid: String,
    private val playlistManager: PlaylistManager,
    private val podcastManager: PodcastManager,
    private val settings: Settings,
) : ViewModel() {
    val bottomInset = settings.bottomInset

    val searchState = SearchFieldState()

    private val playlistFlow = searchState.textFlow.flatMapLatest { searchTerm ->
        playlistManager.manualPlaylistFlow(playlistUuid, searchTerm)
    }

    val uiState = combine(
        playlistFlow,
        podcastManager.countSubscribedFlow(),
    ) { playlist, followedCount ->
        UiState(
            manualPlaylist = playlist,
            isAnyPodcastFollowed = followedCount > 0,
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, initialValue = UiState.Empty)

    data class UiState(
        val manualPlaylist: ManualPlaylist?,
        val isAnyPodcastFollowed: Boolean,
    ) {
        companion object {
            val Empty = UiState(
                manualPlaylist = null,
                isAnyPodcastFollowed = false,
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(playlistUuid: String): PlaylistViewModel
    }
}
