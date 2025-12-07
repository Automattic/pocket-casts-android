package au.com.shiftyjelly.pocketcasts.wear.ui.podcast

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class PodcastViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
    private val theme: Theme,
    settings: Settings,
) : ViewModel() {

    private val podcastUuid: String = savedStateHandle[PodcastScreen.ARGUMENT] ?: ""

    sealed class UiState {
        object Empty : UiState()
        data class Loaded(
            val podcast: Podcast? = null,
            val episodes: List<PodcastEpisode> = emptyList(),
            val theme: Theme,
        ) : UiState()
    }

    val artworkConfiguration = settings.artworkConfiguration.flow

    var uiState: StateFlow<UiState> = flow {
        val podcast = podcastManager.findPodcastByUuid(podcastUuid)
        if (podcast == null) {
            emit(UiState.Empty)
            return@flow
        }

        episodeManager.findEpisodesByPodcastOrderedFlow(podcast)
            .map { episodes ->
                val filtered = episodes.filterNot { it.isArchived || it.isFinished }
                podcast.grouping.sortFunction?.let { sortFunction ->
                    filtered.sortedByDescending(sortFunction)
                } ?: filtered
            }
            .collect { episodes ->
                emit(
                    UiState.Loaded(
                        podcast = podcast,
                        episodes = episodes,
                        theme = theme,
                    ),
                )
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(1000),
        initialValue = UiState.Empty,
    )
}
