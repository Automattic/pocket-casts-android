package au.com.shiftyjelly.pocketcasts.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeItem
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchHistoryEntry
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManager
import au.com.shiftyjelly.pocketcasts.search.SearchResultsFragment.Companion.ResultsType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchHandler: SearchHandler,
    private val searchHistoryManager: SearchHistoryManager,
    private val podcastManager: PodcastManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ViewModel() {
    var isFragmentChangingConfigurations: Boolean = false
    var showSearchHistory: Boolean = true
    private val searchResults = searchHandler.searchResults.map { searchState ->
        val isSearchStarted = (loading.value == true)
        if (isSearchStarted) {
            saveSearchTerm(searchState.searchTerm)
            showSearchHistory = false
        }
        searchState
    }
    val loading = searchHandler.loading
    private var source: AnalyticsSource = AnalyticsSource.UNKNOWN

    private val _state: MutableStateFlow<SearchState> = MutableStateFlow(
        SearchState.Results(
            searchTerm = "",
            podcasts = emptyList(),
            episodes = emptyList(),
            error = null,
            loading = false
        )
    )
    val state: StateFlow<SearchState> = _state

    init {

        viewModelScope.launch {
            val subscribedUuidFlow = podcastManager
                .observeSubscribed()
                .asFlow()
                .map { ls ->
                    ls.map { it.uuid }
                }

            combine(
                subscribedUuidFlow,
                searchResults.asFlow()
            ) { subscribedUuids, searchState ->
                when (searchState) {
                    is SearchState.NoResults -> searchState
                    is SearchState.Results -> {
                        searchState.copy(
                            podcasts = searchState.podcasts
                                .map { podcast ->
                                    if (podcast is FolderItem.Podcast) {
                                        podcast.copy(podcast.podcast.copy(isSubscribed = subscribedUuids.contains(podcast.podcast.uuid)))
                                    } else {
                                        podcast
                                    }
                                }
                        )
                    }
                }
            }.stateIn(viewModelScope).collect {
                _state.value = it
            }
        }
    }

    fun updateSearchQuery(query: String, immediate: Boolean = false) {
        // Prevent updating the search query when navigating back to the search results after tapping on a result.
        if (_state.value.searchTerm == query) return
        searchHandler.updateSearchQuery(query, immediate)
    }

    fun setOnlySearchRemote(remote: Boolean) {
        searchHandler.setOnlySearchRemote(remote)
    }

    fun setSource(source: AnalyticsSource) {
        this.source = source
        searchHandler.setSource(source)
    }

    private fun saveSearchTerm(term: String) {
        viewModelScope.launch {
            searchHistoryManager.add(SearchHistoryEntry.SearchTerm(term = term))
        }
    }

    fun onSubscribeToPodcast(podcast: Podcast) {
        if (podcast.isSubscribed) return
        podcastManager.subscribeToPodcast(podcastUuid = podcast.uuid, sync = true)

        // Optimistically update subscribe status
        val results = _state.value as? SearchState.Results
        results?.copy(
            podcasts = results.podcasts.map {
                if (it is FolderItem.Podcast && it.uuid == podcast.uuid) {
                    it.copy(podcast = podcast.copy(isSubscribed = true))
                } else {
                    it
                }
            }
        )?.let { _state.value = it }
        analyticsTracker.track(
            AnalyticsEvent.PODCAST_SUBSCRIBED,
            AnalyticsProp.podcastSubscribed(uuid = podcast.uuid, source = source)
        )
    }

    fun onFragmentPause(isChangingConfigurations: Boolean?) {
        isFragmentChangingConfigurations = isChangingConfigurations ?: false
    }

    fun trackSearchResultTapped(
        source: AnalyticsSource,
        uuid: String,
        type: SearchResultType
    ) {
        analyticsTracker.track(
            AnalyticsEvent.SEARCH_RESULT_TAPPED,
            AnalyticsProp.searchResultTapped(source = source, uuid = uuid, type = type)
        )
    }

    fun trackSearchShownOrDismissed(
        event: AnalyticsEvent,
        source: AnalyticsSource,
    ) {
        analyticsTracker.track(
            event,
            AnalyticsProp.searchShownOrDismissed(source = source)
        )
    }

    fun trackSearchListShown(source: AnalyticsSource, type: ResultsType) {
        analyticsTracker.track(
            AnalyticsEvent.SEARCH_LIST_SHOWN,
            AnalyticsProp.searchListShown(source = source, type = type)
        )
    }

    enum class SearchResultType(val value: String) {
        PODCAST_LOCAL_RESULT("podcast_local_result"),
        PODCAST_REMOTE_RESULT("podcast_remote_result"),
        FOLDER("folder"),
        EPISODE("episode"),
    }

    private object AnalyticsProp {
        private const val SOURCE = "source"
        private const val UUID = "uuid"
        private const val RESULT_TYPE = "result_type"
        private const val DISPLAYING = "displaying"

        fun searchResultTapped(source: AnalyticsSource, uuid: String, type: SearchResultType) =
            mapOf(SOURCE to source.analyticsValue, UUID to uuid, RESULT_TYPE to type.value)

        fun searchShownOrDismissed(source: AnalyticsSource) =
            mapOf(SOURCE to source.analyticsValue)

        fun podcastSubscribed(source: AnalyticsSource, uuid: String) =
            mapOf(SOURCE to "${source.analyticsValue}_search", UUID to uuid)

        fun searchListShown(source: AnalyticsSource, type: ResultsType) =
            mapOf(SOURCE to source.analyticsValue, DISPLAYING to type.value)
    }
}

sealed class SearchState {
    abstract val searchTerm: String
    data class NoResults(override val searchTerm: String) : SearchState()
    data class Results(
        override val searchTerm: String,
        val podcasts: List<FolderItem>,
        val episodes: List<EpisodeItem>,
        val loading: Boolean,
        val error: Throwable?,
    ) : SearchState()
}
