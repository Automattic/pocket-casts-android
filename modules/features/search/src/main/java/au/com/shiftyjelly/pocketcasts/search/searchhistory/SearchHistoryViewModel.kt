package au.com.shiftyjelly.pocketcasts.search.searchhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.to.SearchHistoryEntry
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SearchHistoryClearedEvent
import com.automattic.eventhorizon.SearchHistoryItemDeleteButtonTappedEvent
import com.automattic.eventhorizon.SearchHistoryItemTappedEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

@HiltViewModel
class SearchHistoryViewModel @Inject constructor(
    private val searchHistoryManager: SearchHistoryManager,
    userManager: UserManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val eventHorizon: EventHorizon,
) : ViewModel() {
    private val signInState = userManager.getSignInState().asFlow()
    private var isSignedInAsPlusOrPatron = false
    private var onlySearchRemote: Boolean = false
    private var source: SourceView = SourceView.UNKNOWN

    private val mutableState = MutableStateFlow(
        State(entries = emptyList()),
    )
    val state: StateFlow<State> = mutableState

    data class State(
        val entries: List<SearchHistoryEntry>,
    )

    fun setOnlySearchRemote(value: Boolean) {
        onlySearchRemote = value
    }

    fun setSource(source: SourceView) {
        this.source = source
    }

    fun start() {
        viewModelScope.launch(ioDispatcher) {
            signInState.collect { signInState ->
                isSignedInAsPlusOrPatron = signInState.isSignedInAsPlusOrPatron
                loadSearchHistory()
            }
        }
    }

    private suspend fun loadSearchHistory() {
        val entries = searchHistoryManager.findAll(
            showFolders = isSignedInAsPlusOrPatron && !onlySearchRemote,
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
            eventHorizon.track(
                SearchHistoryItemDeleteButtonTappedEvent(
                    uuid = entry.uuid(),
                    type = entry.eventHorizonValue,
                    source = source.eventHorizonValue,
                ),
            )
        }
    }

    fun clearAll() {
        viewModelScope.launch(ioDispatcher) {
            searchHistoryManager.clearAll()
            loadSearchHistory()
            eventHorizon.track(
                SearchHistoryClearedEvent(
                    source = source.eventHorizonValue,
                ),
            )
        }
    }

    fun trackHistoryItemTapped(entry: SearchHistoryEntry) {
        eventHorizon.track(
            SearchHistoryItemTappedEvent(
                uuid = entry.uuid(),
                type = entry.eventHorizonValue,
                source = source.eventHorizonValue,
            ),
        )
    }

    private fun SearchHistoryEntry.uuid() = when (this) {
        is SearchHistoryEntry.Episode -> uuid
        is SearchHistoryEntry.Folder -> uuid
        is SearchHistoryEntry.Podcast -> uuid
        is SearchHistoryEntry.SearchTerm -> null
    }
}
