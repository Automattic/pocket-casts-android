package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.PodcastAndEpisode
import au.com.shiftyjelly.pocketcasts.repositories.di.DefaultDispatcher
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

private const val SEARCH_DEBOUNCE = 300L

@HiltViewModel
class TranscriptSearchViewModel @Inject constructor(
    private val kmpSearch: KMPSearch,
    private val analyticsTracker: AnalyticsTracker,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private var _searchSourceText: String = ""
    private var _podcastAndEpisode: PodcastAndEpisode? = null

    private val _searchQueryFlow = MutableStateFlow("")
    val searchQueryFlow = _searchQueryFlow.asStateFlow()

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState = _searchState.asStateFlow()

    init {
        @OptIn(FlowPreview::class)
        _searchQueryFlow
            .debounce(SEARCH_DEBOUNCE)
            .onEach { searchQuery ->
                performSearch(searchQuery)
            }
            .launchIn(viewModelScope)
    }

    fun setSearchInput(
        searchSourceText: String,
        podcastAndEpisode: PodcastAndEpisode?,
    ) {
        resetSearch()
        this._searchSourceText = searchSourceText
        this._podcastAndEpisode = podcastAndEpisode
    }

    fun onSearchQueryChanged(searchQuery: String) {
        _searchQueryFlow.value = searchQuery
    }

    private suspend fun performSearch(searchTerm: String) = withContext(defaultDispatcher) {
        try {
            kmpSearch.setPattern(searchTerm)
            val searchResultIndices = kmpSearch.search(_searchSourceText)
            _searchState.update {
                it.copy(
                    searchTerm = searchTerm,
                    searchResultIndices = searchResultIndices,
                    currentSearchIndex = 0,
                )
            }
        } catch (e: Exception) {
            LogBuffer.e(LogBuffer.TAG_INVALID_STATE, e, "Error searching transcript")
        }
    }

    fun onSearchButtonClicked() {
        track(AnalyticsEvent.TRANSCRIPT_SEARCH_SHOWN)
    }

    fun onSearchPrevious() {
        val currentState = _searchState.value
        if (currentState.searchResultIndices.isEmpty()) return
        val previousIndex = (currentState.currentSearchIndex - 1 + currentState.searchResultIndices.size) % currentState.searchResultIndices.size
        _searchState.update { it.copy(currentSearchIndex = previousIndex) }
        track(AnalyticsEvent.TRANSCRIPT_SEARCH_PREVIOUS_RESULT)
    }

    fun onSearchNext() {
        val currentState = _searchState.value
        if (currentState.searchResultIndices.isEmpty()) return
        val nextIndex = (currentState.currentSearchIndex + 1) % currentState.searchResultIndices.size
        _searchState.update { it.copy(currentSearchIndex = nextIndex) }
        track(AnalyticsEvent.TRANSCRIPT_SEARCH_NEXT_RESULT)
    }

    fun onSearchDone() {
        resetSearch()
    }

    fun onSearchCleared() {
        resetSearch()
    }

    private fun resetSearch() {
        onSearchQueryChanged("")
        _searchState.update {
            it.copy(
                searchTerm = "",
                currentSearchIndex = 0,
                searchResultIndices = emptyList(),
            )
        }
    }

    fun track(
        event: AnalyticsEvent,
    ) {
        analyticsTracker.track(
            event,
            mapOf(
                "episode_uuid" to _podcastAndEpisode?.episodeUuid.orEmpty(),
                "podcast_uuid" to _podcastAndEpisode?.podcast?.uuid.orEmpty(),
            ),
        )
    }

    data class SearchUiState(
        val searchTerm: String = "",
        val searchResultIndices: List<Int> = emptyList(),
        val currentSearchIndex: Int = 0,
    ) {
        val searchOccurrencesText: String
            get() = searchResultIndices
                .takeIf { it.isNotEmpty() }
                ?.let { "${currentSearchIndex + 1}/${searchResultIndices.size}" }
                ?: "0"

        val prevNextArrowButtonsEnabled = searchResultIndices.isNotEmpty()
    }
}
