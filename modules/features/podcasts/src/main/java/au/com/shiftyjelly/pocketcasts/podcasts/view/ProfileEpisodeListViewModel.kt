package au.com.shiftyjelly.pocketcasts.podcasts.view

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.podcasts.view.ProfileEpisodeListFragment.Mode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import au.com.shiftyjelly.pocketcasts.images.R as IR
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

    private var mode: Mode? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    fun setup(mode: Mode) {
        this.mode = mode
        val episodeListFlowable = when (mode) {
            is Mode.Downloaded -> episodeManager.findDownloadEpisodesRxFlowable()
            is Mode.Starred -> episodeManager.findStarredEpisodesRxFlowable()
            is Mode.History -> episodeManager.findPlaybackHistoryEpisodesRxFlowable()
        }
        viewModelScope.launch {
            val searchResultsFlow = _searchQueryFlow
                .flatMapLatest { searchQuery ->
                    if (searchQuery.isNotEmpty()) {
                        episodeManager.filteredPlaybackHistoryEpisodesFlow(searchQuery)
                    } else {
                        flowOf(emptyList())
                    }
                }
            combine(
                episodeListFlowable.asFlow(),
                searchResultsFlow,
            ) { episodeList, searchResults ->
                val searchQuery = searchQueryFlow.value
                val results = if (searchQuery.isNotEmpty()) searchResults else episodeList
                val showSearchBar = mode.showSearch &&
                    (results.isNotEmpty() || searchQuery.isNotEmpty())
                _state.value = if (results.isEmpty()) {
                    State.Empty(
                        icon = State.Empty.iconRes(mode),
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

    fun onSearchQueryChanged(searchQuery: String) {
        val oldValue = _searchQueryFlow.value
        _searchQueryFlow.value = searchQuery

        // Track search events
        if (oldValue.isEmpty() && searchQuery.isNotEmpty()) {
            track(AnalyticsEvent.SEARCH_PERFORMED)
        } else if (oldValue.isNotEmpty() && searchQuery.isEmpty()) {
            track(AnalyticsEvent.SEARCH_CLEARED)
        }
    }

    sealed class State {
        open val showSearchBar: Boolean = false

        data class Loaded(
            override val showSearchBar: Boolean = false,
            val results: List<PodcastEpisode>? = null,
        ) : State()

        data class Empty(
            @DrawableRes val icon: Int,
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

                fun iconRes(mode: Mode): Int =
                    when (mode) {
                        is Mode.Downloaded -> IR.drawable.ic_download
                        is Mode.Starred -> IR.drawable.ic_starred
                        is Mode.History -> IR.drawable.ic_listen_history
                    }
            }
        }

        data object Loading : State()
    }

    private fun track(
        analyticsEvent: AnalyticsEvent,
    ) {
        mode?.let { analyticsTracker.track(analyticsEvent, mapOf("source" to it.source.analyticsValue)) }
    }
}
