package au.com.shiftyjelly.pocketcasts.podcasts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.repositories.di.DefaultDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = TvPodcastDetailsViewModel.Factory::class)
class TvPodcastDetailsViewModel @AssistedInject constructor(
    @Assisted private val podcastUuid: String,
    private val podcastManager: PodcastManager,
    private val episodeManager: EpisodeManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    val uiState: StateFlow<TvPodcastDetailsUiState> = flow {
        val podcast = try {
            podcastManager.findOrDownloadPodcastRxSingle(podcastUuid, waitForSubscribe = false).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "Could not load TV podcast details for $podcastUuid")
            null
        }
        if (podcast == null) {
            emit(TvPodcastDetailsUiState.NotFound)
        } else {
            emitAll(
                podcastManager.podcastByUuidFlow(podcastUuid).flatMapLatest { updatedPodcast ->
                    episodeManager.findEpisodesByPodcastOrderedFlow(updatedPodcast).map { episodes ->
                        TvPodcastDetailsUiState.Loaded(
                            podcast = updatedPodcast,
                            episodes = if (updatedPodcast.showArchived) {
                                episodes
                            } else {
                                episodes.filterNot(PodcastEpisode::isArchived)
                            },
                        )
                    }
                },
            )
        }
    }.flowOn(defaultDispatcher)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(stopTimeout = 300.milliseconds, replayExpiration = Duration.ZERO),
            TvPodcastDetailsUiState.Loading,
        )

    fun changeSortType(sortType: EpisodesSortType) {
        val podcast = (uiState.value as? TvPodcastDetailsUiState.Loaded)?.podcast ?: return
        viewModelScope.launch(defaultDispatcher) {
            podcastManager.updateEpisodesSortTypeBlocking(podcast, sortType)
        }
    }

    fun toggleArchiveFilter() {
        val podcast = (uiState.value as? TvPodcastDetailsUiState.Loaded)?.podcast ?: return
        viewModelScope.launch {
            podcastManager.updateShowArchived(podcast, !podcast.showArchived)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(podcastUuid: String): TvPodcastDetailsViewModel
    }
}

sealed interface TvPodcastDetailsUiState {
    data object Loading : TvPodcastDetailsUiState

    data object NotFound : TvPodcastDetailsUiState

    data class Loaded(
        val podcast: Podcast,
        val episodes: List<PodcastEpisode>,
    ) : TvPodcastDetailsUiState
}
