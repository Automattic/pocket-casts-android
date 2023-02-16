package au.com.shiftyjelly.pocketcasts.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchHistoryEntry
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchHandler: SearchHandler,
    private val searchHistoryManager: SearchHistoryManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ViewModel() {
    var isFragmentChangingConfigurations: Boolean = false
    var showSearchHistory: Boolean = true
    val searchResults = searchHandler.searchResults.map { searchState ->
        val isSearchStarted = (loading.value == true)
        if (isSearchStarted) {
            saveSearchTerm(searchState.searchTerm)
            showSearchHistory = false
        }
        searchState
    }
    val loading = searchHandler.loading

    fun updateSearchQuery(query: String) {
        searchHandler.updateSearchQuery(query)
    }

    fun setOnlySearchRemote(remote: Boolean) {
        searchHandler.setOnlySearchRemote(remote)
    }

    fun setSource(source: AnalyticsSource) {
        searchHandler.setSource(source)
    }

    private fun saveSearchTerm(term: String) {
        viewModelScope.launch {
            searchHistoryManager.add(SearchHistoryEntry.SearchTerm(term = term))
        }
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

    enum class SearchResultType(val value: String) {
        PODCAST_LOCAL_RESULT("podcast_local_result"),
        PODCAST_REMOTE_RESULT("podcast_remote_result"),
        FOLDER("folder"),
    }

    private object AnalyticsProp {
        private const val SOURCE = "source"
        private const val UUID = "uuid"
        private const val RESULT_TYPE = "result_type"
        fun searchResultTapped(source: AnalyticsSource, uuid: String, type: SearchResultType) =
            mapOf(SOURCE to source.analyticsValue, UUID to uuid, RESULT_TYPE to type.value)

        fun searchShownOrDismissed(source: AnalyticsSource) =
            mapOf(SOURCE to source.analyticsValue)
    }
}

sealed class SearchState {
    abstract val searchTerm: String
    data class NoResults(override val searchTerm: String) : SearchState()
    data class Results(
        override val searchTerm: String,
        val list: List<FolderItem>,
        val loading: Boolean,
        val error: Throwable?,
    ) : SearchState()
}
