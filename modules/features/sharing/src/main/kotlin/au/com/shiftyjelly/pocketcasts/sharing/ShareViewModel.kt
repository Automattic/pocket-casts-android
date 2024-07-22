package au.com.shiftyjelly.pocketcasts.sharing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = ShareViewModel.Factory::class)
class ShareViewModel @AssistedInject constructor(
    @Assisted initialPodcast: Podcast,
    @Assisted initialEpisode: PodcastEpisode,
    @Assisted observePlayback: Boolean,
    private val playbackManager: PlaybackManager,
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
) : ViewModel() {
    val uiState = if (observePlayback) {
        @OptIn(ExperimentalCoroutinesApi::class)
        playbackManager.playbackStateFlow.distinctUntilChangedBy { listOf(it.podcast?.uuid, it.episodeUuid) }.flatMapLatest { playbackState ->
            combine(
                flowOf(playbackState.podcast),
                episodeManager.observeEpisodeByUuid(playbackState.episodeUuid),
                ::UiState,
            )
        }
    } else {
        combine(
            podcastManager.observePodcastByUuidFlow(initialPodcast.uuid),
            episodeManager.observeEpisodeByUuid(initialEpisode.uuid),
            ::UiState,
        )
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = UiState(initialPodcast, initialEpisode))

    data class UiState(
        val podcast: Podcast?,
        val episode: BaseEpisode,
    )

    @AssistedFactory
    interface Factory {
        fun create(
            initialPodcast: Podcast,
            initialEpisode: PodcastEpisode,
            observePlayback: Boolean,
        ): ShareViewModel
    }
}
