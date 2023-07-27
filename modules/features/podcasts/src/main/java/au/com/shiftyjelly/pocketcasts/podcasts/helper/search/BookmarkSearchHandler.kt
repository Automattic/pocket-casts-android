package au.com.shiftyjelly.pocketcasts.podcasts.helper.search

import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import io.reactivex.Observable
import io.reactivex.Single
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkSearchHandler @Inject constructor(
    private val bookmarkManager: BookmarkManager,
) : SearchHandler<Bookmark>() {
    override val searchDebounce = 500L

    override fun getSearchResultsObservable(podcastUuid: String): Observable<SearchResult> =
        searchQueryRelay.debounce { // Only debounce when search has a value otherwise it slows down loading the pages
            if (it.isEmpty()) {
                Observable.empty()
            } else {
                Observable.timer(searchDebounce, TimeUnit.MILLISECONDS)
            }
        }.switchMapSingle { searchTerm ->
            if (searchTerm.length > 1) {
                Single.just(bookmarkManager.searchInPodcastByTitle(podcastUuid, searchTerm))
                    .map { SearchResult(searchTerm, it) }
                    .onErrorReturnItem(noSearchResult)
            } else {
                Single.just(noSearchResult)
            }
        }.distinctUntilChanged()

    override fun trackSearchIfNeeded(oldValue: String, newValue: String) {
        // TODO: Bookmark search tracking
    }
}
