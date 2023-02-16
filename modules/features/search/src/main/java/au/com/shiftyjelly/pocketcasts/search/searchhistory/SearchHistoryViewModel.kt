package au.com.shiftyjelly.pocketcasts.search.searchhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ViewModel() {
    private val signInState = userManager.getSignInState().asFlow()
    private var isSignedAsPlus = false
    private var onlySearchRemote: Boolean = false
    private var source: AnalyticsSource = AnalyticsSource.UNKNOWN

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

    fun setSource(source: AnalyticsSource) {
        this.source = source
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
            trackEventForEntry(AnalyticsEvent.SEARCH_HISTORY_ITEM_DELETE_BUTTON_TAPPED, entry)
        }
    }

    fun clearAll() {
        viewModelScope.launch(ioDispatcher) {
            searchHistoryManager.clearAll()
            loadSearchHistory()
            analyticsTracker.track(
                AnalyticsEvent.SEARCH_HISTORY_CLEARED,
                AnalyticsProp.sourceMap(source = source)
            )
        }
    }

    fun trackEventForEntry(event: AnalyticsEvent, entry: SearchHistoryEntry) {
        val type = entry.type()
        analyticsTracker.track(
            event,
            AnalyticsProp.searchHistoryEntryMap(
                source = source,
                type = type,
                uuid = entry.uuid()
            )
        )
    }

    private fun SearchHistoryEntry.type() = when (this) {
        is SearchHistoryEntry.Episode -> SearchHistoryType.EPISODE
        is SearchHistoryEntry.Folder -> SearchHistoryType.FOLDER
        is SearchHistoryEntry.Podcast -> SearchHistoryType.PODCAST
        is SearchHistoryEntry.SearchTerm -> SearchHistoryType.SEARCH_TERM
    }

    private fun SearchHistoryEntry.uuid() = when (this) {
        is SearchHistoryEntry.Episode -> uuid
        is SearchHistoryEntry.Folder -> uuid
        is SearchHistoryEntry.Podcast -> uuid
        is SearchHistoryEntry.SearchTerm -> null
    }

    companion object {
        private object AnalyticsProp {
            const val SOURCE = "source"
            const val TYPE = "type"
            const val UUID = "uuid"
            fun sourceMap(source: AnalyticsSource) = mapOf(SOURCE to source.analyticsValue)
            fun searchHistoryEntryMap(
                source: AnalyticsSource,
                type: SearchHistoryType,
                uuid: String? = null,
            ) = HashMap<String, String>().apply {
                put(SOURCE, source.analyticsValue)
                put(TYPE, type.value)
                uuid?.let { put(UUID, it) }
            } as Map<String, String>
        }

        enum class SearchHistoryType(val value: String) {
            EPISODE("episode"),
            FOLDER("folder"),
            PODCAST("podcast"),
            SEARCH_TERM("search_term"),
        }
    }
}
