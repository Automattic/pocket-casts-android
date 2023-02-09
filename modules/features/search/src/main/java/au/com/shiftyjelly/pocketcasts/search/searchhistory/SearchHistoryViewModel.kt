package au.com.shiftyjelly.pocketcasts.search.searchhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.SearchHistoryEntry
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SEARCH_HISTORY_LIMIT = 5

@HiltViewModel
class SearchHistoryViewModel @Inject constructor(
    private val searchHistoryManager: SearchHistoryManager,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        State(entries = emptyList())
    )
    val state: StateFlow<State> = mutableState

    data class State(
        val entries: List<SearchHistoryEntry>,
    )

    fun start() {
        viewModelScope.launch {
            loadSearchHistory()
        }
    }

    private suspend fun loadSearchHistory() {
        val entries = searchHistoryManager.findAll(
            showFolders = false,
            limit = SEARCH_HISTORY_LIMIT
        )
        mutableState.value = mutableState.value.copy(entries = entries)
    }

    fun add(entry: SearchHistoryEntry) {
        viewModelScope.launch {
            searchHistoryManager.add(entry)
        }
    }

    fun remove(entry: SearchHistoryEntry) {
        viewModelScope.launch {
            searchHistoryManager.remove(entry)
            loadSearchHistory()
        }
    }
}
