package au.com.shiftyjelly.pocketcasts.podcasts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.preferences.TvPreferences
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
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
    private val preferences: TvPreferences,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val isShowingArchivedFlow = MutableStateFlow(preferences.isPodcastShowingArchived(podcastUuid))

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
            val podcastEpisodesFlow = podcastManager.podcastByUuidFlow(podcastUuid)
                .filterNotNull()
                .flatMapLatest { updatedPodcast ->
                    episodeManager.findEpisodesByPodcastOrderedFlow(updatedPodcast)
                        .map { episodes -> updatedPodcast to episodes }
                }
            emitAll(
                combine(podcastEpisodesFlow, isShowingArchivedFlow) { (loadedPodcast, episodes), isShowingArchived ->
                    TvPodcastDetailsUiState.Loaded(
                        podcast = loadedPodcast,
                        episodes = if (isShowingArchived) episodes else episodes.filterNot(PodcastEpisode::isArchived),
                        archivedEpisodeCount = episodes.count(PodcastEpisode::isArchived),
                        isShowingArchived = isShowingArchived,
                    )
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
        val isShowingArchived = !isShowingArchivedFlow.value
        preferences.setPodcastShowingArchived(podcastUuid, isShowingArchived)
        isShowingArchivedFlow.value = isShowingArchived
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
        val archivedEpisodeCount: Int,
        val isShowingArchived: Boolean,
    ) : TvPodcastDetailsUiState
}
