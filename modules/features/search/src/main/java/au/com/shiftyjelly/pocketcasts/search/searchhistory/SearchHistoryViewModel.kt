package au.com.shiftyjelly.pocketcasts.search.searchhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.SearchHistoryEntry
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.ui.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import javax.inject.Inject

@HiltViewModel
class SearchHistoryViewModel @Inject constructor(
    private val searchHistoryManager: SearchHistoryManager,
    userManager: UserManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val signInState = userManager.getSignInState().asFlow()
    private var isSignedAsPlus = false
    private var onlySearchRemote: Boolean = false

    private val mutableState = MutableStateFlow(
        State(entries = emptyList())
    )
    val state: StateFlow<State> = mutableState

    data class State(
        val entries: List<SearchHistoryEntry>,
    )

    fun setOnlySearchRemote(value: Boolean) {
        onlySearchRemote = value
    }

    fun start() {
        viewModelScope.launch(ioDispatcher) {
            signInState.collect { signInState ->
                isSignedAsPlus = signInState.isSignedInAsPlus
                loadSearchHistory()
            }
        }
    }

    private suspend fun loadSearchHistory() {
        val entries = searchHistoryManager.findAll(
            showFolders = isSignedAsPlus && !onlySearchRemote
        )
        mutableState.value = mutableState.value.copy(entries = entries)
    }

    fun add(entry: SearchHistoryEntry) {
        viewModelScope.launch(ioDispatcher) {
            searchHistoryManager.add(entry)
        }
    }

    fun remove(entry: SearchHistoryEntry) {
        viewModelScope.launch(ioDispatcher) {
            searchHistoryManager.remove(entry)
            loadSearchHistory()
        }
    }

    fun clearAll() {
        viewModelScope.launch(ioDispatcher) {
            searchHistoryManager.clearAll()
            loadSearchHistory()
        }
    }
}
