package au.com.shiftyjelly.pocketcasts.wear.ui.podcast

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PodcastViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val podcastManager: PodcastManager,
    private val episodeManager: EpisodeManager,
) : ViewModel() {

    private val podcastUuid: String = savedStateHandle[PodcastScreen.argument] ?: ""

    data class UiState(
        val podcast: Podcast? = null,
        val episodes: List<Episode> = emptyList(),
    )

    var uiState by mutableStateOf(UiState())
        private set

    init {
        viewModelScope.launch(Dispatchers.Default) {
            val podcast = podcastManager.findPodcastByUuidSuspend(podcastUuid)
            val episodes = podcast?.let {
                episodeManager.findEpisodesByPodcastOrdered(it)
            } ?: emptyList()
            uiState = UiState(
                podcast = podcast,
                episodes = episodes
            )
        }
    }
}
