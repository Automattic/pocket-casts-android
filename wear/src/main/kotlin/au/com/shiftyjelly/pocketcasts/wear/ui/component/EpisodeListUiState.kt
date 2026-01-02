package au.com.shiftyjelly.pocketcasts.wear.ui.component

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode

sealed class EpisodeListUiState {
    data object Loading : EpisodeListUiState()
    data object Empty : EpisodeListUiState()
    data class Loaded(
        val episodes: List<PodcastEpisode>,
    ) : EpisodeListUiState()
}
