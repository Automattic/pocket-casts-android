package au.com.shiftyjelly.pocketcasts.wear.ui.filter

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SmartPlaylistManager
import au.com.shiftyjelly.pocketcasts.wear.ui.filters.FiltersViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.reactive.asFlow

@HiltViewModel
class FilterViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val smartPlaylistManager: SmartPlaylistManager,
    private val episodeManager: EpisodeManager,
    private val playbackManager: PlaybackManager,
    settings: Settings,
) : ViewModel() {

    private val filterUuid: String = savedStateHandle[FilterScreen.ARGUMENT_FILTER_UUID] ?: ""

    sealed class UiState {
        object Loading : UiState()
        data class Empty(
            val filter: SmartPlaylist? = null,
        ) : UiState()
        data class Loaded(
            val filter: SmartPlaylist,
            val episodes: List<PodcastEpisode>,
        ) : UiState()
    }

    val uiState = smartPlaylistManager.findByUuidAsListRxFlowable(filterUuid)
        .switchMap { filters ->
            val filter = filters.firstOrNull()
            if (filter != null) {
                smartPlaylistManager.observeEpisodesBlocking(filter, episodeManager, playbackManager)
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

    val artworkConfiguration = settings.artworkConfiguration.flow
}
