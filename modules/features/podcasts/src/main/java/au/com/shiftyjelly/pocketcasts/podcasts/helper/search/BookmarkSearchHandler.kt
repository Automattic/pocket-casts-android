package au.com.shiftyjelly.pocketcasts.podcasts.helper.search

import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import io.reactivex.Observable
import kotlinx.coroutines.rx2.rxSingle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkSearchHandler @Inject constructor(
    private val bookmarkManager: BookmarkManager,
) : SearchHandler<Bookmark>() {

    override fun getSearchResultsObservable(podcastUuid: String): Observable<SearchResult> =
        searchQueryRelay.switchMapSingle { searchTerm ->
            if (searchTerm.length > 1) {
                rxSingle { bookmarkManager.searchInPodcastByTitle(podcastUuid, searchTerm) }
                    .map { SearchResult(searchTerm, it) }
                    .onErrorReturnItem(noSearchResult)
            } else {
                rxSingle { noSearchResult }
            }
        }.distinctUntilChanged()

    override fun trackSearchIfNeeded(oldValue: String, newValue: String) {
        // TODO: Bookmark search tracking
    }
}
