package au.com.shiftyjelly.pocketcasts.player.viewmodel

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.player.util.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class BookmarksViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var bookmarkManager: BookmarkManager

    @Mock
    private lateinit var episodeManager: EpisodeManager

    @Mock
    private lateinit var episode: BaseEpisode

    private lateinit var bookmarksViewModel: BookmarksViewModel
    private val episodeUuid = UUID.randomUUID().toString()

    @Before
    fun setUp() = runTest {
        whenever(episodeManager.findEpisodeByUuid(episodeUuid)).thenReturn(episode)

        bookmarksViewModel = BookmarksViewModel(
            bookmarkManager = bookmarkManager,
            episodeManager = episodeManager,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `given no bookmarks, when bookmarks loaded, then Empty state shown`() = runTest {
        whenever(bookmarkManager.findEpisodeBookmarks(episode)).thenReturn(flowOf(emptyList()))

        bookmarksViewModel.loadBookmarks(episodeUuid)

        assertTrue(bookmarksViewModel.uiState.value is BookmarksViewModel.UiState.Empty)
    }

    @Test
    fun `given bookmarks present, when bookmarks loaded, then Loaded state shown`() = runTest {
        whenever(bookmarkManager.findEpisodeBookmarks(episode)).thenReturn(flowOf(listOf(mock())))

        bookmarksViewModel.loadBookmarks(episodeUuid)

        assertTrue(bookmarksViewModel.uiState.value is BookmarksViewModel.UiState.Loaded)
    }
}
