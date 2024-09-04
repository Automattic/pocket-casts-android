package au.com.shiftyjelly.pocketcasts.podcasts.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

@HiltViewModel
class ProfileEpisodeListViewModel @Inject constructor(
    val episodeManager: EpisodeManager,
    val playbackManager: PlaybackManager,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel(), CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val _searchQueryFlow = MutableStateFlow("")
    val searchQueryFlow = _searchQueryFlow.asStateFlow()

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.Loading)
    val state: StateFlow<State> = _state

    @OptIn(ExperimentalCoroutinesApi::class)
    fun setup(mode: ProfileEpisodeListFragment.Mode) {
        val episodeListFlowable = when (mode) {
            is ProfileEpisodeListFragment.Mode.Downloaded -> episodeManager.observeDownloadEpisodes()
            is ProfileEpisodeListFragment.Mode.Starred -> episodeManager.observeStarredEpisodes()
            is ProfileEpisodeListFragment.Mode.History -> episodeManager.observePlaybackHistoryEpisodes()
        }
        viewModelScope.launch {
            val searchResultsFlow = _searchQueryFlow
                .flatMapLatest { searchQuery ->
                    episodeManager.filteredPlaybackHistoryEpisodesFlow(searchQuery)
                }
            combine(
                episodeListFlowable.asFlow(),
                searchResultsFlow,
            ) { episodeList, searchResults ->
                val results = if (searchQueryFlow.value.isNotEmpty()) searchResults else episodeList
                _state.value = if (results.isEmpty()) {
                    State.Empty
                } else {
                    State.Loaded(
                        showSearch = mode.showSearch
                                && FeatureFlag.isEnabled(Feature.SEARCH_IN_LISTENING_HISTORY)
                                && results.isNotEmpty(),
                        results = results,
                    )
                }
            }.stateIn(viewModelScope)
        }
    }

    fun clearAllEpisodeHistory() {
        launch {
            analyticsTracker.track(AnalyticsEvent.LISTENING_HISTORY_CLEARED)
            episodeManager.clearAllEpisodeHistory()
        }
    }

    fun updateSearchQuery(searchQuery: String) {
        _searchQueryFlow.value = searchQuery.trim()
    }

    sealed class State {
        data class Loaded(
            val showSearch: Boolean = false,
            val results: List<PodcastEpisode>? = null,
        ) : State()

        data object Empty : State()

        data object Loading : State()
    }
}
