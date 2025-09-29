package au.com.shiftyjelly.pocketcasts.repositories.search

import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem
import au.com.shiftyjelly.pocketcasts.servers.search.AutoCompleteResult
import au.com.shiftyjelly.pocketcasts.servers.search.SearchService
import io.reactivex.Observable
import javax.inject.Inject
import kotlinx.coroutines.rx2.rxObservable

class SearchAutoCompleteManagerImpl @Inject constructor(
    private val searchService: SearchService,
) : SearchAutoCompleteManager {
    override suspend fun autoCompleteSearch(term: String): List<SearchAutoCompleteItem> {
        val response = searchService.autocompleteSearch(query = term, limit = 10, items = 10, language = "en-GB") // TODO
        return response.results.map {
            when (it) {
                is AutoCompleteResult.TermResult -> SearchAutoCompleteItem.Term(term = it.value)
                is AutoCompleteResult.PodcastResult -> SearchAutoCompleteItem.Podcast(uuid = it.value.uuid, title = it.value.title, author = it.value.author)
            } as SearchAutoCompleteItem
        }
    }

    override fun autoCompleteSearchRxObservable(term: String): Observable<List<SearchAutoCompleteItem>> {
        return rxObservable {
            autoCompleteSearch(term)
        }
    }
}