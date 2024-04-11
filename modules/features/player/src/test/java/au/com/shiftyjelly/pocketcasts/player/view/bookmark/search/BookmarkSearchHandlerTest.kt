package au.com.shiftyjelly.pocketcasts.player.view.bookmark.search

import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class BookmarkSearchHandlerTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var bookmarkManager: BookmarkManager

    private lateinit var bookmarkSearchHandler: BookmarkSearchHandler

    @Before
    fun setUp() {
        bookmarkSearchHandler = BookmarkSearchHandler(bookmarkManager)
    }

    @Test
    fun `given non-empty search term, when search query updated, then search results flow emits correct value`() = runTest {
        val searchTerm = "test"
        val searchUuids = listOf("uuid1", "uuid2")
        whenever(bookmarkManager.searchByBookmarkOrEpisodeTitle(searchTerm)).thenReturn(searchUuids)

        bookmarkSearchHandler.searchQueryUpdated(searchTerm)

        val result = bookmarkSearchHandler.getBookmarkSearchResultsFlow().first()
        assertEquals(searchTerm, result.searchTerm)
        assertEquals(searchUuids, result.searchUuids)
    }

    @Test
    fun `given empty search term, when search query updated, then search results flow emits no search result`() = runTest {
        val searchTerm = ""

        bookmarkSearchHandler.searchQueryUpdated(searchTerm)

        val result = bookmarkSearchHandler.getBookmarkSearchResultsFlow().first()
        assertEquals(searchTerm, result.searchTerm)
        assertNull(result.searchUuids)
    }

    @Test
    fun `given search term causes error, when search query updated, then search results flow emits no search result`() = runTest {
        val searchTerm = "test"
        whenever(bookmarkManager.searchByBookmarkOrEpisodeTitle(searchTerm)).thenThrow(RuntimeException())

        bookmarkSearchHandler.searchQueryUpdated(searchTerm)

        val result = bookmarkSearchHandler.getBookmarkSearchResultsFlow().first()
        assertEquals(searchTerm, result.searchTerm)
        assertNull(result.searchUuids)
    }
}
