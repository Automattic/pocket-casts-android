package au.com.shiftyjelly.pocketcasts.wear.ui.podcast

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import javax.inject.Inject

@HiltViewModel
class PodcastViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
    private val theme: Theme,
) : ViewModel() {

    private val podcastUuid: String = savedStateHandle[PodcastScreen.argument] ?: ""

    sealed class UiState {
        object Empty : UiState()
        data class Loaded(
            val podcast: Podcast? = null,
            val episodes: List<PodcastEpisode> = emptyList(),
            val theme: Theme,
        ) : UiState()
    }

    var uiState: UiState by mutableStateOf(UiState.Empty)
        private set

    init {
        viewModelScope.launch(Dispatchers.Default) {
            val podcast = podcastManager.findPodcastByUuidSuspend(podcastUuid)
            podcast?.let {
                episodeManager.observeEpisodesByPodcastOrderedRx(it)
                    .asFlow()
                    .map { podcastEpisodes ->
                        val sortFunction = podcast.podcastGrouping.sortFunction
                        if (sortFunction != null) {
                            podcastEpisodes.sortedByDescending(sortFunction)
                        } else {
                            podcastEpisodes
                        }
                    }
                    .stateIn(viewModelScope)
                    .collect { episodes ->
                        uiState = UiState.Loaded(
                            podcast = podcast,
                            episodes = episodes,
                            theme = theme,
                        )
                    }
            }
        }
    }
}
