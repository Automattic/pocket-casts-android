package au.com.shiftyjelly.pocketcasts.sharing.episode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = ShareEpisodeViewModel.Factory::class)
class ShareEpisodeViewModel @AssistedInject constructor(
    @Assisted episodeUuid: String,
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
    private val settings: Settings,
) : ViewModel() {
    val uiState = combine(
        podcastManager.observePodcastByEpisodeUuid(episodeUuid),
        episodeManager.observeByUuid(episodeUuid),
        settings.artworkConfiguration.flow.map { it.useEpisodeArtwork },
        ::UiState,
    ).stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = UiState())

    data class UiState(
        val podcast: Podcast? = null,
        val episode: PodcastEpisode? = null,
        val useEpisodeArtwork: Boolean = false,
    )

    @AssistedFactory
    interface Factory {
        fun create(episodeUuid: String): ShareEpisodeViewModel
    }
}
