package au.com.shiftyjelly.pocketcasts.podcasts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.onboarding.signin.TvSignInUiState
import au.com.shiftyjelly.pocketcasts.onboarding.signin.deviceAuthFlow
import au.com.shiftyjelly.pocketcasts.preferences.TvPreferences
import au.com.shiftyjelly.pocketcasts.repositories.di.DefaultDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.PodcastScreenShownEvent
import com.automattic.eventhorizon.PodcastScreenSubscribeTappedEvent
import com.automattic.eventhorizon.PodcastScreenToggleArchivedEvent
import com.automattic.eventhorizon.PodcastScreenToggleSummaryEvent
import com.automattic.eventhorizon.PodcastScreenUnsubscribeTappedEvent
import com.automattic.eventhorizon.PodcastSubscribedEvent
import com.automattic.eventhorizon.PodcastUnsubscribedEvent
import com.automattic.eventhorizon.PodcastsScreenSortOrderChangedEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = TvPodcastDetailsViewModel.Factory::class)
class TvPodcastDetailsViewModel @AssistedInject constructor(
    @Assisted private val podcastUuid: String,
    @Assisted private val source: SourceView,
    private val podcastManager: PodcastManager,
    private val episodeManager: EpisodeManager,
    private val syncManager: SyncManager,
    private val preferences: TvPreferences,
    private val eventHorizon: EventHorizon,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val isShowingArchivedFlow = MutableStateFlow(preferences.isPodcastShowingArchived(podcastUuid))

    private val _accountAuthState = MutableStateFlow<TvSignInUiState>(TvSignInUiState.Loading)
    val accountAuthState: StateFlow<TvSignInUiState> = _accountAuthState.asStateFlow()
    private var accountAuthJob: Job? = null

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
            val podcastFlow = podcastManager.podcastByUuidFlow(podcastUuid).filterNotNull()
            val episodesFlow = podcastFlow
                .distinctUntilChangedBy { it.episodesSortType }
                .flatMapLatest { episodeManager.findEpisodesByPodcastOrderedFlow(it) }
            val isLoggedInFlow = syncManager.isLoggedInObservable.asFlow()
            emitAll(
                combine(podcastFlow, episodesFlow, isShowingArchivedFlow, isLoggedInFlow) { loadedPodcast, episodes, isShowingArchived, isLoggedIn ->
                    TvPodcastDetailsUiState.Loaded(
                        podcast = loadedPodcast,
                        episodes = if (isShowingArchived) episodes else episodes.filterNot(PodcastEpisode::isArchived),
                        archivedEpisodeCount = episodes.count(PodcastEpisode::isArchived),
                        isShowingArchived = isShowingArchived,
                        isLoggedIn = isLoggedIn,
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

    fun trackScreenShown() {
        eventHorizon.track(PodcastScreenShownEvent(source = source.analyticsValue))
    }

    fun trackSummaryExpanded() {
        eventHorizon.track(PodcastScreenToggleSummaryEvent(isExpanded = true))
    }

    fun startAccountAuth() {
        accountAuthJob?.cancel()
        _accountAuthState.value = TvSignInUiState.Loading
        accountAuthJob = viewModelScope.launch {
            deviceAuthFlow(syncManager, isNewAccount = false).collect { state ->
                _accountAuthState.value = state
                if (state is TvSignInUiState.Complete) {
                    podcastManager.subscribeToPodcast(podcastUuid, sync = true)
                    trackSubscribed()
                    // Sibling of accountAuthJob so the modal dismiss cancel doesn't kill the refresh.
                    viewModelScope.launch {
                        try {
                            podcastManager.refreshPodcastsAfterSignIn()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "Failed to refresh podcasts after TV account sign in")
                        }
                    }
                }
            }
        }
    }

    fun retryAccountAuth() {
        startAccountAuth()
    }

    fun stopAccountAuth() {
        accountAuthJob?.cancel()
        accountAuthJob = null
    }

    fun toggleSubscribe() {
        val podcast = (uiState.value as? TvPodcastDetailsUiState.Loaded)?.podcast ?: return
        if (podcast.isSubscribed) {
            eventHorizon.track(PodcastScreenUnsubscribeTappedEvent)
            eventHorizon.track(PodcastUnsubscribedEvent(uuid = podcastUuid, source = SourceView.PODCAST_SCREEN.analyticsValue))
            viewModelScope.launch(ioDispatcher) {
                podcastManager.unsubscribe(podcastUuid, SourceView.PODCAST_SCREEN)
            }
        } else {
            eventHorizon.track(PodcastScreenSubscribeTappedEvent)
            podcastManager.subscribeToPodcast(podcastUuid, sync = true)
            trackSubscribed()
        }
    }

    private fun trackSubscribed() {
        eventHorizon.track(PodcastSubscribedEvent(uuid = podcastUuid, source = SourceView.PODCAST_SCREEN.analyticsValue))
    }

    fun changeSortType(sortType: EpisodesSortType) {
        eventHorizon.track(PodcastsScreenSortOrderChangedEvent(sortOrder = sortType.analyticsValue))
        viewModelScope.launch(ioDispatcher) {
            podcastManager.updateEpisodesSortTypeBlocking(Podcast(uuid = podcastUuid), sortType)
        }
    }

    fun toggleArchiveFilter() {
        val isShowingArchived = !isShowingArchivedFlow.value
        eventHorizon.track(PodcastScreenToggleArchivedEvent(showArchived = isShowingArchived))
        preferences.setPodcastShowingArchived(podcastUuid, isShowingArchived)
        isShowingArchivedFlow.value = isShowingArchived
    }

    @AssistedFactory
    interface Factory {
        fun create(podcastUuid: String, source: SourceView): TvPodcastDetailsViewModel
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
        val isLoggedIn: Boolean,
    ) : TvPodcastDetailsUiState
}
