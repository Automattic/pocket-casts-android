package au.com.shiftyjelly.pocketcasts.sharing.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = SharePodcastViewModel.Factory::class)
class SharePodcastViewModel @AssistedInject constructor(
    @Assisted podcastUuid: String,
    private val podcastManager: PodcastManager,
) : ViewModel() {
    val uiState = combine(
        podcastManager.observePodcastByUuidFlow(podcastUuid),
        podcastManager.observeEpisodeCountByPodcatUuid(podcastUuid),
        ::UiState,
    ).stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = UiState())

    data class UiState(
        val podcast: Podcast? = null,
        val episodeCount: Int = 0,
    )

    @AssistedFactory
    interface Factory {
        fun create(podcastUuid: String): SharePodcastViewModel
    }
}
