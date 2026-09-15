package au.com.shiftyjelly.pocketcasts.podcasts.helper.search

import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn

class BookmarkSearchHandler @Inject constructor(
    private val bookmarkManager: BookmarkManager,
) : SearchHandler<Bookmark>() {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getSearchResultsFlow(podcastUuid: String): Flow<SearchResult> = searchQueryFlow
        .flatMapLatest { searchTerm ->
            if (searchTerm.length > 1) {
                flow {
                    val bookmarkUuids = bookmarkManager.searchInPodcastByTitle(podcastUuid, searchTerm)
                    emit(SearchResult(searchTerm, bookmarkUuids))
                }.catch { emit(noSearchResult) }
            } else {
                flowOf(noSearchResult)
            }
        }
        // Results used to come from rxSingle on Dispatchers.Default, keep them off the caller's thread
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()

    override fun trackSearchIfNeeded(oldValue: String, newValue: String) {
        // TODO: Bookmark search tracking
    }
}
