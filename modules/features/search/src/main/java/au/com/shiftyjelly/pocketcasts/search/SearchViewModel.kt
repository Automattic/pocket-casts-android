package au.com.shiftyjelly.pocketcasts.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeItem
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.to.ImprovedSearchResultItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchHistoryEntry
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManager
import au.com.shiftyjelly.pocketcasts.search.SearchResultsFragment.Companion.ResultsType
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

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
        if (FeatureFlag.isEnabled(Feature.IMPROVED_SEARCH_SUGGESTIONS)) {
            viewModelScope.launch {
                searchHandler.searchSuggestions
                    .collect { operation ->
                        showSearchHistory = false
                        _state.update { uiState ->
                            when (operation) {
                                is SearchUiState.SearchOperation.Error -> {
                                    analyticsTracker.track(
                                        AnalyticsEvent.IMPROVED_SEARCH_SUGGESTIONS_FAILED,
                                        mapOf(
                                            "source" to source.analyticsValue,
                                            "term" to operation.searchTerm,
                                        ),
                                    )
                                }
                                is SearchUiState.SearchOperation.Success -> {
                                    if (operation.results.isEmpty()) {
                                        analyticsTracker.track(
                                            AnalyticsEvent.IMPROVED_SEARCH_EMPTY_RESULTS,
                                            mapOf(
                                                "source" to source.analyticsValue,
                                                "term" to operation.searchTerm,
                                            ),
                                        )
                                    }
                                }

                                else -> Unit
                            }

                            // only show loading for the initial query when autocomplete results are empty
                            if (((uiState as? SearchUiState.Suggestions)?.operation as? SearchUiState.SearchOperation.Success)?.results?.isNotEmpty() == true && operation is SearchUiState.SearchOperation.Loading) {
                                uiState
                            } else {
                                SearchUiState.Suggestions(operation = operation)
                            }
                        }
                    }
            }
        }

        viewModelScope.launch {
            if (FeatureFlag.isEnabled(Feature.IMPROVED_SEARCH_RESULTS)) {
                searchHandler.improvedSearchResults.collect {
                    showSearchHistory = false
                    _state.value = SearchUiState.ImprovedResults(operation = it)
                    if (!FeatureFlag.isEnabled(Feature.IMPROVED_SEARCH_SUGGESTIONS) && it is SearchUiState.SearchOperation.Loading) {
                        saveSearchTerm(it.searchTerm)
                    }
                }
            } else {
                searchHandler.searchResults.collect {
                    showSearchHistory = false
                    if (_state.value is SearchUiState.OldResults) {
                        _state.value = SearchUiState.OldResults(operation = it)
                    }
                    if (!FeatureFlag.isEnabled(Feature.IMPROVED_SEARCH_SUGGESTIONS) && it is SearchUiState.SearchOperation.Loading) {
                        saveSearchTerm(it.searchTerm)
                    }
                }
            }
        }
    }

    fun updateSearchQuery(query: String, immediate: Boolean = false) {
        // Prevent updating the search query when navigating back to the search results after tapping on a result.
        if (query == _state.value.searchTerm) return

        if (FeatureFlag.isEnabled(Feature.IMPROVED_SEARCH_SUGGESTIONS)) {
            searchHandler.updateAutCompleteQuery(query)
            _state.update {
                if ((it is SearchUiState.OldResults || it is SearchUiState.ImprovedResults) && it.searchTerm.orEmpty().length > query.length) {
                    SearchUiState.Suggestions(operation = SearchUiState.SearchOperation.Success(searchTerm = query, results = emptyList()))
                } else {
                    it
                }
            }
        } else {
            searchHandler.updateSearchQuery(query, immediate)
        }
    }

    fun setOnlySearchRemote(remote: Boolean) {
        searchHandler.setOnlySearchRemote(remote)
    }

    fun setSource(source: SourceView) {
        this.source = source
        searchHandler.setSource(source)
    }

    private fun saveSearchTerm(term: String) {
        if (term.isBlank()) return
        viewModelScope.launch {
            searchHistoryManager.add(SearchHistoryEntry.SearchTerm(term = term))
        }
    }

    fun onSubscribeToPodcast(podcast: Podcast) {
        if (podcast.isSubscribed) return
        onSubscribeToPodcast(podcast.uuid)
    }

    fun selectFilter(filter: ResultsFilters) {
        if (FeatureFlag.isEnabled(Feature.IMPROVED_SEARCH_RESULTS) && _state.value is SearchUiState.ImprovedResults) {
            analyticsTracker.track(
                AnalyticsEvent.IMPROVED_SEARCH_FILTER_TAPPED,
                mapOf(
                    "source" to source.analyticsValue,
                    "filter" to filter.name,
                ),
            )
            _state.update { state ->
                when (state) {
                    is SearchUiState.ImprovedResults -> {
                        if (state.operation is SearchUiState.SearchOperation.Success) {
                            state.copy(
                                selectedFilterIndex = ResultsFilters.entries.indexOf(filter),
                                operation = state.operation.copy(
                                    results = state.operation.results.copy(filter = filter),
                                ),
                            )
                        } else {
                            state
                        }
                    }

                    else -> state
                }
            }
        }
    }

    fun onSubscribeToPodcast(uuid: String) {
        podcastManager.subscribeToPodcast(podcastUuid = uuid, sync = true)

        // Optimistically update subscribe status
        _state.update {
            when (it) {
                is SearchUiState.OldResults ->
                    it.copy(
                        operation = (it.operation as? SearchUiState.SearchOperation.Success)?.copy(
                            results = it.operation.results.subscribeToPodcast(uuid),
                        ) ?: it.operation,
                    )

                is SearchUiState.ImprovedResults ->
                    it.copy(
                        operation = (it.operation as? SearchUiState.SearchOperation.Success)?.copy(
                            results = it.operation.results.subscribeToPodcast(uuid),
                        ) ?: it.operation,
                    )

                is SearchUiState.Suggestions -> it.copy(
                    operation = (it.operation as? SearchUiState.SearchOperation.Success)?.copy(
                        results = it.operation.results.map { autoCompleteItem ->
                            if (autoCompleteItem is SearchAutoCompleteItem.Podcast && autoCompleteItem.uuid == uuid) {
                                autoCompleteItem.copy(isSubscribed = true)
                            } else {
                                autoCompleteItem
                            }
                        },
                    ) ?: it.operation,
                )

                else -> it
            }
        }

        analyticsTracker.track(
            AnalyticsEvent.PODCAST_SUBSCRIBED,
            AnalyticsProp.podcastSubscribed(uuid = uuid, source = source),
        )
    }

    fun runSearchOnTerm(term: String) {
        saveSearchTerm(term)
        searchHandler.updateSearchQuery(term, true)

        _state.value = if (FeatureFlag.isEnabled(Feature.IMPROVED_SEARCH_RESULTS)) {
            SearchUiState.ImprovedResults(operation = SearchUiState.SearchOperation.Loading(term))
        } else {
            SearchUiState.OldResults(operation = SearchUiState.SearchOperation.Loading(term))
        }
    }

    fun selectSuggestion(suggestion: String) {
        analyticsTracker.track(
            AnalyticsEvent.IMPROVED_SEARCH_SUGGESTION_TERM_TAPPED,
            properties = mapOf(
                "term" to suggestion,
                "source" to source.analyticsValue,
            ),
        )

        runSearchOnTerm(suggestion)
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

    fun trackSuggestionsShown() {
        analyticsTracker.track(
            AnalyticsEvent.IMPROVED_SEARCH_SUGGESTIONS_SHOWN,
            mapOf(
                "source" to source.analyticsValue,
            ),
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

sealed interface SearchResults {
    val isEmpty: Boolean

    fun subscribeToPodcast(uuid: String): SearchResults

    data class SegregatedResults(
        val podcasts: List<FolderItem>,
        val episodes: List<EpisodeItem>,
    ) : SearchResults {
        override val isEmpty: Boolean get() = podcasts.isEmpty() && episodes.isEmpty()

        override fun subscribeToPodcast(uuid: String) = copy(
            podcasts = podcasts.map { folderItem ->
                if (folderItem.uuid == uuid) {
                    (folderItem as? FolderItem.Podcast)?.copy(podcast = folderItem.podcast.copy(isSubscribed = true)) ?: folderItem
                } else {
                    folderItem
                }
            },
        )
    }

    data class ImprovedResults(
        val results: List<ImprovedSearchResultItem>,
        val filter: ResultsFilters,
    ) : SearchResults {
        val filteredResults: List<ImprovedSearchResultItem>
            get() = results.filter { item ->
                when (filter) {
                    ResultsFilters.TOP_RESULTS -> true
                    ResultsFilters.EPISODES -> item is ImprovedSearchResultItem.EpisodeItem
                    ResultsFilters.PODCASTS -> item is ImprovedSearchResultItem.PodcastItem || item is ImprovedSearchResultItem.FolderItem
                }
            }

        override val isEmpty: Boolean get() = filteredResults.isEmpty()

        override fun subscribeToPodcast(uuid: String) = copy(results = results.map { if (it is ImprovedSearchResultItem.PodcastItem && it.uuid == uuid) it.copy(isFollowed = true) else it })
    }
}

enum class ResultsFilters(val resId: Int) {
    TOP_RESULTS(LR.string.search_filters_top_results),
    PODCASTS(LR.string.search_filters_podcasts),
    EPISODES(LR.string.search_filters_episodes),
}

sealed interface SearchUiState {

    val searchTerm: String?
        get() = when (this) {
            is Suggestions -> operation.searchTerm
            is OldResults -> operation.searchTerm
            is ImprovedResults -> operation.searchTerm
            else -> null
        }

    val isLoading: Boolean
        get() = when (this) {
            is Suggestions -> operation is SearchOperation.Loading
            is OldResults -> operation is SearchOperation.Loading
            is ImprovedResults -> operation is SearchOperation.Loading
            else -> false
        }

    sealed interface SearchOperation<out T> {
        val searchTerm: String

        data class Loading(override val searchTerm: String) : SearchOperation<Nothing>
        data class Error(override val searchTerm: String, val error: Throwable) : SearchOperation<Nothing>
        data class Success<T>(override val searchTerm: String, val results: T) : SearchOperation<T>
    }

    data object Idle : SearchUiState
    data class Suggestions(val operation: SearchOperation<List<SearchAutoCompleteItem>>) : SearchUiState
    data class ImprovedResults(
        val operation: SearchOperation<SearchResults.ImprovedResults>,
        val filterOptions: Set<ResultsFilters> = ResultsFilters.entries.toSet(),
        val selectedFilterIndex: Int = 0,
    ) : SearchUiState

    data class OldResults(
        val operation: SearchOperation<SearchResults.SegregatedResults>,
    ) : SearchUiState
}
