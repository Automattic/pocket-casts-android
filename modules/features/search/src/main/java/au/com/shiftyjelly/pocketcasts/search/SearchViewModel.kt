package au.com.shiftyjelly.pocketcasts.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeItem
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchHistoryEntry
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManager
import au.com.shiftyjelly.pocketcasts.search.SearchResultsFragment.Companion.ResultsType
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asObservable

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchHandler: SearchHandler,
    private val searchHistoryManager: SearchHistoryManager,
    private val podcastManager: PodcastManager,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    var isFragmentChangingConfigurations: Boolean = false
    var showSearchHistory: Boolean = true

    private var source: SourceView = SourceView.UNKNOWN

    private val _state: MutableStateFlow<SearchUiState> = MutableStateFlow(
        SearchUiState.Idle,
    )
    val state: StateFlow<SearchUiState> = _state

    init {

        viewModelScope.launch {
            searchHandler.searchSuggestions.collect {
                Log.i("===", "VM.searchSuggestion = $it")
                if (_state.value is SearchUiState.Idle || _state.value is SearchUiState.Suggestions) {
                    _state.value = SearchUiState.Suggestions(operation = it)
                }
            }

            searchHandler.searchResults.subscribe {
                if (_state.value is SearchUiState.Results) {
                    _state.value = SearchUiState.Results(operation = it as SearchUiState.SearchOperation<SearchResults>)
                }
            }
        }

//        viewModelScope.launch {
//            val subscribedUuidFlow = podcastManager
//                .subscribedRxFlowable()
//                .asFlow()
//                .map { ls ->
//                    ls.map { it.uuid }
//                }
//            combine(
//                subscribedUuidFlow,
//                searchResults.asFlow(),
//            ) { subscribedUuids, searchState ->
//                if (searchState is SearchUiState.Success.SearchResults) {
//                    searchState.copy(
//                        podcasts = searchState.podcasts
//                            .map { podcast ->
//                                if (podcast is FolderItem.Podcast) {
//                                    podcast.copy(podcast.podcast.copy(isSubscribed = subscribedUuids.contains(podcast.podcast.uuid)))
//                                } else {
//                                    podcast
//                                }
//                            },
//                    )
//                } else {
//                    searchState
//                }
//            }.stateIn(viewModelScope).collect {
//                _state.value = it
//            }
//        }
    }

    fun updateSearchQuery(query: String, immediate: Boolean = false) {
        // Prevent updating the search query when navigating back to the search results after tapping on a result.
//        if (_state.value.searchTerm == query) return
        searchHandler.updateSearchQuery(query, immediate)
    }

    fun setOnlySearchRemote(remote: Boolean) {
        searchHandler.setOnlySearchRemote(remote)
    }

    fun setSource(source: SourceView) {
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
        analyticsTracker.track(
            AnalyticsEvent.PODCAST_SUBSCRIBED,
            AnalyticsProp.podcastSubscribed(uuid = podcast.uuid, source = source),
        )
    }

    fun onFragmentPause(isChangingConfigurations: Boolean?) {
        isFragmentChangingConfigurations = isChangingConfigurations ?: false
    }

    fun trackSearchResultTapped(
        source: SourceView,
        uuid: String,
        type: SearchResultType,
    ) {
        analyticsTracker.track(
            AnalyticsEvent.SEARCH_RESULT_TAPPED,
            AnalyticsProp.searchResultTapped(source = source, uuid = uuid, type = type),
        )
    }

    fun trackSearchShownOrDismissed(
        event: AnalyticsEvent,
        source: SourceView,
    ) {
        analyticsTracker.track(
            event,
            AnalyticsProp.searchShownOrDismissed(source = source),
        )
    }

    fun trackSearchListShown(source: SourceView, type: ResultsType) {
        analyticsTracker.track(
            AnalyticsEvent.SEARCH_LIST_SHOWN,
            AnalyticsProp.searchListShown(source = source, type = type),
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

        fun searchResultTapped(source: SourceView, uuid: String, type: SearchResultType) = mapOf(SOURCE to source.analyticsValue, UUID to uuid, RESULT_TYPE to type.value)

        fun searchShownOrDismissed(source: SourceView) = mapOf(SOURCE to source.analyticsValue)

        fun podcastSubscribed(source: SourceView, uuid: String) = mapOf(SOURCE to "${source.analyticsValue}_search", UUID to uuid)

        fun searchListShown(source: SourceView, type: ResultsType) = mapOf(SOURCE to source.analyticsValue, DISPLAYING to type.value)
    }
}

data class SearchResults(
    val podcasts: List<FolderItem>,
    val episodes: List<EpisodeItem>,
) {
    val isEmpty: Boolean get() = podcasts.isEmpty() && episodes.isEmpty()
}

sealed interface SearchUiState {

    val searchTerm: String? get() = when (this) {
        is Suggestions -> operation.searchTerm
        is Results -> operation.searchTerm
        else -> null
    }

    val isLoading: Boolean get() = when (this) {
        is Suggestions -> operation is SearchOperation.Loading
        is Results -> operation is SearchOperation.Loading
        else -> false
    }

    sealed interface SearchOperation<T> {
        val searchTerm: String
        data class Loading(override val searchTerm: String) : SearchOperation<Nothing>
        data class Error(override val searchTerm: String, val error: Throwable) : SearchOperation<Nothing>
        data class Results<T>(override val searchTerm: String, val results: T) : SearchOperation<T>
    }

    data object Idle : SearchUiState
    data class Suggestions(val operation: SearchOperation<List<SearchAutoCompleteItem>>) : SearchUiState
    data class Results(val operation: SearchOperation<SearchResults>) : SearchUiState
}
