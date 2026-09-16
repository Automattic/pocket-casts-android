package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.automattic.eventhorizon.EventHorizon
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarkViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val episodeManager = mock<EpisodeManager>()
    private val userEpisodeManager = mock<UserEpisodeManager>()
    private val bookmarkManager = mock<BookmarkManager>()

    private val viewModel = BookmarkViewModel(episodeManager, userEpisodeManager, bookmarkManager, EventHorizon(TestEventSink()))

    private val episodeUuid = "episode-id"
    private val timeSecs = 120

    @Before
    fun setUp() {
        FeatureFlag.setEnabled(Feature.SMART_BOOKMARKS, true)
    }

    @Test
    fun `titles a freshly created bookmark in the adding state`() = runTest {
        whenever(bookmarkManager.findBookmark("new-id")).thenReturn(Bookmark(uuid = "new-id", title = "Bookmark"))

        viewModel.load(newBookmarkArguments("new-id"))

        val state = viewModel.uiState.value
        assertTrue(state.isNewBookmark)
        assertEquals("Bookmark", state.title.text)
    }

    @Test
    fun `edits an existing bookmark in the change state`() = runTest {
        whenever(bookmarkManager.findBookmark("existing-id")).thenReturn(Bookmark(uuid = "existing-id", title = "Kept"))

        viewModel.load(existingBookmarkArguments("existing-id"))

        assertFalse(viewModel.uiState.value.isNewBookmark)
    }

    @Test
    fun `saves a freshly created bookmark as added`() = runTest {
        whenever(bookmarkManager.findBookmark("new-id")).thenReturn(Bookmark(uuid = "new-id", title = "Bookmark"))

        viewModel.load(newBookmarkArguments("new-id"))
        val saved = CompletableDeferred<Boolean>()
        viewModel.saveBookmark { _, isExisting -> saved.complete(isExisting) }

        assertFalse(saved.await())
    }

    @Test
    fun `discards the bookmark when a freshly created one is dismissed`() = runTest {
        whenever(bookmarkManager.findBookmark("new-id")).thenReturn(Bookmark(uuid = "new-id", title = "Bookmark"))

        viewModel.load(newBookmarkArguments("new-id"))
        viewModel.discardNewBookmarkIfNeeded()

        verify(bookmarkManager).deleteToSync("new-id")
    }

    @Test
    fun `keeps the bookmark when an existing one is dismissed`() = runTest {
        whenever(bookmarkManager.findBookmark("existing-id")).thenReturn(Bookmark(uuid = "existing-id", title = "Kept"))

        viewModel.load(existingBookmarkArguments("existing-id"))
        viewModel.discardNewBookmarkIfNeeded()

        verify(bookmarkManager, never()).deleteToSync(any())
    }

    private fun newBookmarkArguments(bookmarkUuid: String) = BookmarkArguments(
        bookmarkUuid = bookmarkUuid,
        episodeUuid = episodeUuid,
        timeSecs = timeSecs,
        podcastColors = PodcastColors.ForUserEpisode,
        isNewBookmark = true,
    )

    private fun existingBookmarkArguments(bookmarkUuid: String) = BookmarkArguments(
        bookmarkUuid = bookmarkUuid,
        episodeUuid = episodeUuid,
        timeSecs = timeSecs,
        podcastColors = PodcastColors.ForUserEpisode,
        isNewBookmark = false,
    )
}
