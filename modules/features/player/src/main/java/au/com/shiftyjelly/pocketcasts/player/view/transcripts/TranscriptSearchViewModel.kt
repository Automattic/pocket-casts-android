package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

private const val SEARCH_DEBOUNCE = 300L

@HiltViewModel
class TranscriptSearchViewModel @Inject constructor() : ViewModel() {
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

    fun onSearchQueryChanged(searchQuery: String) {
        _searchQueryFlow.value = searchQuery
    }

    @Suppress("UNUSED_PARAMETER")
    private fun performSearch(searchTerm: String) {
    }

    fun onSearchPrevious() {
        val currentState = _searchState.value
        val previousIndex = (currentState.currentSearchIndex - 1 + currentState.searchResultIndices.size) % currentState.searchResultIndices.size
        _searchState.update { it.copy(currentSearchIndex = previousIndex) }
    }

    fun onSearchNext() {
        val currentState = _searchState.value
        val nextIndex = (currentState.currentSearchIndex + 1) % currentState.searchResultIndices.size
        _searchState.update { it.copy(currentSearchIndex = nextIndex) }
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

    data class SearchUiState(
        val searchTerm: String = "",
        val searchResultIndices: List<Int> = emptyList(),
        val currentSearchIndex: Int = 0,
    ) {
        val searchOccurencesText: String
            get() = searchResultIndices
                .takeIf { it.isNotEmpty() }
                ?.let { "${currentSearchIndex + 1}/${it.size}" }
                ?: ""
    }
}
