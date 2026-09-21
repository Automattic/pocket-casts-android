package au.com.shiftyjelly.pocketcasts.podcasts.helper.search

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

abstract class SearchHandler<T> {
    protected val searchQueryFlow = MutableStateFlow("")

    protected val noSearchResult = SearchResult("", null)

    abstract fun getSearchResultsFlow(podcastUuid: String): Flow<SearchResult>

    fun searchQueryUpdated(newValue: String) {
        val oldValue = searchQueryFlow.value
        searchQueryFlow.value = newValue
        trackSearchIfNeeded(oldValue, newValue)
    }

    abstract fun trackSearchIfNeeded(oldValue: String, newValue: String)

    data class SearchResult(
        val searchTerm: String,
        val searchUuids: List<String>?,
    )
}
