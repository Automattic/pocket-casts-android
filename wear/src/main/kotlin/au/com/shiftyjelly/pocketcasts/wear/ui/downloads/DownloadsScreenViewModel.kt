package au.com.shiftyjelly.pocketcasts.wear.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class DownloadsScreenViewModel @Inject constructor(
    episodeManager: EpisodeManager,
    settings: Settings,
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        object Empty : UiState()
        data class Loaded(
            val episodes: List<PodcastEpisode>,
        ) : UiState()
    }

    val stateFlow: StateFlow<UiState> = episodeManager.findDownloadEpisodesFlow().map { episodes ->
        if (episodes.isEmpty()) {
            UiState.Empty
        } else {
            UiState.Loaded(episodes)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(1000),
        initialValue = UiState.Loading,
    )

    val artworkConfiguration = settings.artworkConfiguration.flow
}
