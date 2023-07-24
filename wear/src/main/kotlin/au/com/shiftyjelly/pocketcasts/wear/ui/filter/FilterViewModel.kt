package au.com.shiftyjelly.pocketcasts.wear.ui.filter

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.wear.ui.filters.FiltersViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.reactive.asFlow
import javax.inject.Inject

@HiltViewModel
class FilterViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistManager: PlaylistManager,
    private val episodeManager: EpisodeManager,
    private val playbackManager: PlaybackManager,
) : ViewModel() {

    private val filterUuid: String = savedStateHandle[FilterScreen.argumentFilterUuid] ?: ""

    sealed class UiState {
        object Loading : UiState()
        data class Empty(
            val filter: Playlist? = null,
        ) : UiState()
        data class Loaded(
            val filter: Playlist,
            val episodes: List<PodcastEpisode>,
        ) : UiState()
    }

    val uiState = playlistManager.observeByUuidAsList(filterUuid)
        .switchMap { filters ->
            val filter = filters.firstOrNull()
            if (filter != null) {
                playlistManager.observeEpisodes(filter, episodeManager, playbackManager)
                    .map {
                        if (it.isEmpty()) UiState.Empty(filter) else UiState.Loaded(filter = filter, episodes = it)
                    }
            } else {
                Flowable.just(UiState.Empty(null))
            }
        }
        .distinctUntilChanged()
        .subscribeOn(Schedulers.io())
        .asFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, FiltersViewModel.UiState.Loading)
}
