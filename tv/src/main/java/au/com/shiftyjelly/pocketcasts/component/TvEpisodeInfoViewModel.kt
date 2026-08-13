package au.com.shiftyjelly.pocketcasts.component

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.shownotes.ShowNotesManager
import au.com.shiftyjelly.pocketcasts.servers.shownotes.ShowNotesState
import com.automattic.eventhorizon.EpisodeDetailShownEvent
import com.automattic.eventhorizon.EpisodeViewSourceType
import com.automattic.eventhorizon.EventHorizon
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TvEpisodeInfoViewModel @Inject constructor(
    private val podcastManager: PodcastManager,
    private val showNotesManager: ShowNotesManager,
    private val eventHorizon: EventHorizon,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState?>(null)
    val uiState: StateFlow<UiState?> = _uiState.asStateFlow()

    private var loadedEpisodeUuid: String? = null

    fun trackShown(source: EpisodeViewSourceType) {
        eventHorizon.track(EpisodeDetailShownEvent(source = source))
    }

    fun load(podcastUuid: String, episodeUuid: String) {
        if (episodeUuid == loadedEpisodeUuid) {
            return
        }
        loadedEpisodeUuid = episodeUuid
        _uiState.value = UiState(episodeUuid = episodeUuid, podcastTitle = null, showNotes = ShowNotes.Loading)
        viewModelScope.launch {
            val title = podcastManager.findPodcastByUuid(podcastUuid)?.title
            updateFor(episodeUuid) { it.copy(podcastTitle = title) }
        }
        viewModelScope.launch {
            val showNotes = when (val state = showNotesManager.loadShowNotes(podcastUuid, episodeUuid)) {
                is ShowNotesState.Loaded -> state.showNotes.takeIf { it.isNotBlank() }?.let(ShowNotes::Loaded) ?: ShowNotes.Unavailable
                else -> ShowNotes.Unavailable
            }
            if (showNotes is ShowNotes.Unavailable && loadedEpisodeUuid == episodeUuid) {
                loadedEpisodeUuid = null
            }
            updateFor(episodeUuid) { it.copy(showNotes = showNotes) }
        }
    }

    private fun updateFor(episodeUuid: String, transform: (UiState) -> UiState) {
        _uiState.update { current ->
            if (current?.episodeUuid == episodeUuid) transform(current) else current
        }
    }

    data class UiState(
        val episodeUuid: String,
        val podcastTitle: String?,
        val showNotes: ShowNotes,
    )

    sealed interface ShowNotes {
        data object Loading : ShowNotes

        data class Loaded(val html: String) : ShowNotes

        data object Unavailable : ShowNotes
    }
}
