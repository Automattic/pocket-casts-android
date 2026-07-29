package au.com.shiftyjelly.pocketcasts.podcasts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class TvYourPodcastsViewModel @Inject constructor(
    private val podcastManager: PodcastManager,
) : ViewModel() {

    val uiState: StateFlow<TvYourPodcastsUiState> = podcastManager.findSubscribedFlow()
        .map { podcasts ->
            val sorted = podcasts.sortedWith(PodcastsSortType.NAME_A_TO_Z.podcastComparator)
            if (sorted.isEmpty()) {
                TvYourPodcastsUiState.Empty
            } else {
                TvYourPodcastsUiState.Loaded(sorted)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(stopTimeout = 300.milliseconds),
            TvYourPodcastsUiState.Loading,
        )

    fun onShown() {
        podcastManager.refreshPodcasts(fromLog = "tv your podcasts")
    }
}

sealed interface TvYourPodcastsUiState {
    data object Loading : TvYourPodcastsUiState

    data object Empty : TvYourPodcastsUiState

    data class Loaded(
        val podcasts: List<Podcast>,
    ) : TvYourPodcastsUiState
}
