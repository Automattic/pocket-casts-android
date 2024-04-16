package au.com.shiftyjelly.pocketcasts.player.view.bookmark.search

import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class BookmarkSearchHandler @Inject constructor(
    private val bookmarkManager: BookmarkManager,
) {
    private var searchTerm = ""
    private val searchQueryFlow = MutableStateFlow("")
    private val noSearchResult = SearchResult("", null)

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getBookmarkSearchResultsFlow() = searchQueryFlow.flatMapLatest { searchTerm ->
        if (searchTerm.isNotEmpty()) {
            flow {
                emit(bookmarkManager.searchByBookmarkOrEpisodeTitle(searchTerm))
            }
                .map { SearchResult(searchTerm, it) }
                .catch { emit(noSearchResult) }
        } else {
            flowOf(noSearchResult)
        }
    }.distinctUntilChanged()

    fun searchQueryUpdated(newValue: String) {
        searchTerm = newValue
        searchQueryFlow.value = newValue
    }

    data class SearchResult(
        val searchTerm: String,
        val searchUuids: List<String>?,
    )
}
