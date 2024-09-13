package au.com.shiftyjelly.pocketcasts.podcasts.view

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.podcasts.view.ProfileEpisodeListFragment.Mode
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
import au.com.shiftyjelly.pocketcasts.localization.R as LR

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
    fun setup(mode: Mode) {
        val episodeListFlowable = when (mode) {
            is Mode.Downloaded -> episodeManager.observeDownloadEpisodes()
            is Mode.Starred -> episodeManager.observeStarredEpisodes()
            is Mode.History -> episodeManager.observePlaybackHistoryEpisodes()
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
                val searchQuery = searchQueryFlow.value
                val results = if (searchQuery.isNotEmpty()) searchResults else episodeList
                val showSearchBar = mode.showSearch &&
                    FeatureFlag.isEnabled(Feature.SEARCH_IN_LISTENING_HISTORY) &&
                    (results.isNotEmpty() || searchQuery.isNotEmpty())
                _state.value = if (results.isEmpty()) {
                    State.Empty(
                        titleRes = State.Empty.titleRes(mode, searchQuery.isNotEmpty()),
                        summaryRes = State.Empty.summaryRes(mode, searchQuery.isNotEmpty()),
                        showSearchBar = showSearchBar,
                    )
                } else {
                    State.Loaded(
                        showSearchBar = showSearchBar,
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
        open val showSearchBar: Boolean = false

        data class Loaded(
            override val showSearchBar: Boolean = false,
            val results: List<PodcastEpisode>? = null,
        ) : State()

        data class Empty(
            @StringRes val titleRes: Int,
            @StringRes val summaryRes: Int,
            override val showSearchBar: Boolean = false,
        ) : State() {
            companion object {
                fun titleRes(mode: Mode, isSearchEmpty: Boolean): Int = if (isSearchEmpty) {
                    LR.string.search_episodes_not_found_title
                } else {
                    when (mode) {
                        is Mode.Downloaded -> LR.string.profile_empty_downloaded
                        is Mode.Starred -> LR.string.profile_empty_starred
                        is Mode.History -> LR.string.profile_empty_history
                    }
                }

                fun summaryRes(mode: Mode, isSearchEmpty: Boolean): Int = if (isSearchEmpty) {
                    LR.string.search_episodes_not_found_summary
                } else {
                    when (mode) {
                        is Mode.Downloaded -> LR.string.profile_empty_downloaded_summary
                        is Mode.Starred -> LR.string.profile_empty_starred_summary
                        is Mode.History -> LR.string.profile_empty_history_summary
                    }
                }
            }
        }

        data object Loading : State()
    }
}
