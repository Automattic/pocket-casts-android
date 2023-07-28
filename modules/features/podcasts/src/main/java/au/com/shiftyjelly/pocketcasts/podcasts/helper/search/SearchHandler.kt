package au.com.shiftyjelly.pocketcasts.podcasts.helper.search

import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable

abstract class SearchHandler<T> {
    private var searchTerm = ""
    protected val searchQueryRelay = BehaviorRelay.create<String>()
        .apply { accept("") }

    protected val noSearchResult = SearchResult("", null)

    abstract fun getSearchResultsObservable(podcastUuid: String): Observable<SearchResult>

    fun searchQueryUpdated(newValue: String) {
        val oldValue = searchQueryRelay.value ?: ""
        searchTerm = newValue
        searchQueryRelay.accept(newValue)
        trackSearchIfNeeded(oldValue, newValue)
    }

    abstract fun trackSearchIfNeeded(oldValue: String, newValue: String)

    data class SearchResult(
        val searchTerm: String,
        val searchUuids: List<String>?,
    )
}
