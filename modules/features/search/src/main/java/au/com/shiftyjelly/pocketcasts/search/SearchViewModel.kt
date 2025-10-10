package au.com.shiftyjelly.pocketcasts.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeItem
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchHistoryEntry
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManager
import au.com.shiftyjelly.pocketcasts.search.SearchResultsFragment.Companion.ResultsType
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.rxMaybe
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchHandler: SearchHandler,
    private val searchHistoryManager: SearchHistoryManager,
    private val podcastManager: PodcastManager,
    private val analyticsTracker: AnalyticsTracker,
    private val episodeManager: EpisodeManager,
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
                        _state.update {
                            if (it is SearchUiState.Idle || it is SearchUiState.Suggestions) {
                                when (operation) {
                                    is SearchUiState.SearchOperation.Error -> {
                                        analyticsTracker.track(
                                            AnalyticsEvent.IMPROVED_SEARCH_SUGGESTIONS_FAILED,
                                            mapOf(
                                                "source" to "discover",
                                            ),
                                        )
                                    }
                                    else -> Unit
                                }

                                // only show loading for the initial query when autocomplete results are empty
                                if (((it as? SearchUiState.Suggestions)?.operation as? SearchUiState.SearchOperation.Success)?.results?.isNotEmpty() == true && operation is SearchUiState.SearchOperation.Loading) {
                                    it
                                } else {
                                    SearchUiState.Suggestions(operation = operation)
                                }
                            } else {
                                it
                            }
                        }
                    }
            }
        }

        viewModelScope.launch {
            searchHandler.searchResults.collect {
                if (_state.value is SearchUiState.Results || !FeatureFlag.isEnabled(Feature.IMPROVED_SEARCH_SUGGESTIONS)) {
                    _state.value = SearchUiState.Results(operation = it as SearchUiState.SearchOperation<SearchResults>)
                }

                if (!FeatureFlag.isEnabled(Feature.IMPROVED_SEARCH_SUGGESTIONS) && it is SearchUiState.SearchOperation.Loading) {
                    saveSearchTerm(it.searchTerm)
                    showSearchHistory = false
                }
            }
        }
    }

    fun updateSearchQuery(query: String, immediate: Boolean = false) {
        // Prevent updating the search query when navigating back to the search results after tapping on a result.
        if (query == _state.value.searchTerm) return

        if (FeatureFlag.isEnabled(Feature.IMPROVED_SEARCH_SUGGESTIONS) && source == SourceView.DISCOVER) {
            searchHandler.updateAutCompleteQuery(query)
            _state.update {
                if (it is SearchUiState.Results && it.searchTerm.orEmpty().length > query.length) {
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
        viewModelScope.launch {
            searchHistoryManager.add(SearchHistoryEntry.SearchTerm(term = term))
        }
    }

    fun onSubscribeToPodcast(podcast: Podcast) {
        if (podcast.isSubscribed) return
        onSubscribeToPodcast(podcast.uuid)
    }

    suspend fun fetchEpisode(episodeItem: EpisodeItem): BaseEpisode? {
        return podcastManager.findOrDownloadPodcastRxSingle(episodeItem.podcastUuid)
            .flatMapMaybe {
                rxMaybe {
                    episodeManager.findByUuid(episodeItem.uuid)
                }
            }
            .toFlowable().asFlow().firstOrNull()
    }

    fun selectFilter(filter: ResultsFilters) {
        if (FeatureFlag.isEnabled(Feature.IMPROVED_SEARCH_RESULTS) && _state.value is SearchUiState.Results) {
            analyticsTracker.track(
                AnalyticsEvent.IMPROVED_SEARCH_FILTER_TAPPED,
                mapOf(
                    "source" to "discover",
                    "filter" to filter.name,
                ),
            )
            _state.update {
                (it as SearchUiState.Results).copy(
                    selectedFilterIndex = ResultsFilters.entries.indexOf(filter),
                )
            }
        }
    }

    fun onSubscribeToPodcast(uuid: String) {
        podcastManager.subscribeToPodcast(podcastUuid = uuid, sync = true)

        // Optimistically update subscribe status
        _state.update {
            when (it) {
                is SearchUiState.Results -> it.copy(
                    operation = (it.operation as? SearchUiState.SearchOperation.Success)?.copy(
                        results = it.operation.results.copy(
                            podcasts = it.operation.results.podcasts.map { folderItem ->
                                if (folderItem.uuid == uuid) {
                                    (folderItem as? FolderItem.Podcast)?.copy(podcast = folderItem.podcast.copy(isSubscribed = true)) ?: folderItem
                                } else {
                                    folderItem
                                }
                            },
                        ),
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

    fun selectSuggestion(suggestion: String) {
        saveSearchTerm(suggestion)
        searchHandler.updateSearchQuery(suggestion, true)

        analyticsTracker.track(
            AnalyticsEvent.IMPROVED_SEARCH_SUGGESTION_TERM_TAPPED,
            properties = mapOf(
                "term" to suggestion,
                "source" to "discover",
            ),
        )

        _state.value = SearchUiState.Results(operation = SearchUiState.SearchOperation.Loading(suggestion))
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
                "source" to "discover",
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

data class SearchResults(
    val podcasts: List<FolderItem>,
    val episodes: List<EpisodeItem>,
) {
    val isEmpty: Boolean get() = podcasts.isEmpty() && episodes.isEmpty()
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
            is Results -> operation.searchTerm
            else -> null
        }

    val isLoading: Boolean
        get() = when (this) {
            is Suggestions -> operation is SearchOperation.Loading
            is Results -> operation is SearchOperation.Loading
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
    data class Results(
        val operation: SearchOperation<SearchResults>,
        val filterOptions: Set<ResultsFilters> = ResultsFilters.entries.toSet(),
        val selectedFilterIndex: Int = 0,
    ) : SearchUiState
}
