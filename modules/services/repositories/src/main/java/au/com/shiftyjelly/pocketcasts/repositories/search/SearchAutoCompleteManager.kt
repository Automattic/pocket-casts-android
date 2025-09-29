package au.com.shiftyjelly.pocketcasts.repositories.search

import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem
import io.reactivex.Observable

interface SearchAutoCompleteManager {
    suspend fun autoCompleteSearch(term: String): List<SearchAutoCompleteItem>
    fun autoCompleteSearchRxObservable(term: String): Observable<List<SearchAutoCompleteItem>>
}