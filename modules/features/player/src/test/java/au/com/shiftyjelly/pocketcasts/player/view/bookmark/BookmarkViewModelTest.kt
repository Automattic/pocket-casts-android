package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.content.Context
import androidx.compose.ui.text.input.TextFieldValue
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkSuggestion
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.shownotes.ShowNotesManager
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.automattic.eventhorizon.EventHorizon
import java.util.Date
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
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarkViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val episodeManager = mock<EpisodeManager>()
    private val userEpisodeManager = mock<UserEpisodeManager>()
    private val bookmarkManager = mock<BookmarkManager>()
    private val transcriptManager = mock<TranscriptManager>()
    private val showNotesManager = mock<ShowNotesManager>()

    private val context = mock<Context> {
        on { getString(LR.string.bookmark) } doReturn "Bookmark"
    }
    private val viewModel = BookmarkViewModel(episodeManager, userEpisodeManager, bookmarkManager, transcriptManager, showNotesManager, EventHorizon(TestEventSink()), context)

    private val episodeUuid = "episode-id"
    private val timeSecs = 120
    private val arguments = BookmarkArguments(
        bookmarkUuid = null,
        episodeUuid = episodeUuid,
        timeSecs = timeSecs,
        podcastColors = PodcastColors.ForUserEpisode,
    )
    private val suggestion = BookmarkSuggestion(passage = "the passage", passageLocation = 5, referenceTimeSecs = 118, title = "A great moment")

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
    fun `suggests a title for a transcript bookmark from its stored passage`() = runTest {
        whenever(bookmarkManager.findBookmark("new-id")).thenReturn(Bookmark(uuid = "new-id", title = "Bookmark", passage = "a captured passage"))
        whenever(bookmarkManager.suggestTitle("a captured passage")).thenReturn("A great moment")

        viewModel.load(newBookmarkArguments("new-id"))

        val state = viewModel.uiState.value
        assertEquals("A great moment", state.title.text)
        assertEquals(BookmarkViewModel.TitleSuggestion.None, state.titleSuggestion)
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

    @Test
    fun `auto-applies the suggested title when the title is untouched`() = runTest {
        stubNewBookmark()
        whenever(bookmarkManager.suggestBookmark(episodeUuid, timeSecs)).thenReturn(suggestion)

        viewModel.load(arguments)

        val state = viewModel.uiState.value
        assertEquals("A great moment", state.title.text)
        assertEquals(BookmarkViewModel.TitleSuggestion.None, state.titleSuggestion)
    }

    @Test
    fun `offers to edit the transcript once a passage is suggested`() = runTest {
        stubNewBookmark()
        whenever(bookmarkManager.suggestBookmark(episodeUuid, timeSecs)).thenReturn(suggestion)

        viewModel.load(arguments)

        assertTrue(viewModel.uiState.value.canEditTranscript)
    }

    @Test
    fun `offers the suggestion and cancels generating when the title is edited`() = runTest {
        stubNewBookmark()
        val gate = CompletableDeferred<BookmarkSuggestion?>()
        doSuspendableAnswer { gate.await() }.whenever(bookmarkManager).suggestBookmark(episodeUuid, timeSecs)

        viewModel.load(arguments)
        assertEquals(BookmarkViewModel.TitleSuggestion.Generating, viewModel.uiState.value.titleSuggestion)

        viewModel.changeTitle(TextFieldValue("My own title"))
        assertEquals(BookmarkViewModel.TitleSuggestion.None, viewModel.uiState.value.titleSuggestion)

        gate.complete(suggestion)

        val state = viewModel.uiState.value
        assertEquals(BookmarkViewModel.TitleSuggestion.Available("A great moment"), state.titleSuggestion)
        assertEquals("My own title", state.title.text)
    }

    @Test
    fun `saves the captured passage with the bookmark`() = runTest {
        stubNewBookmark()
        whenever(bookmarkManager.suggestBookmark(episodeUuid, timeSecs)).thenReturn(suggestion)
        whenever(episodeManager.findByUuid(episodeUuid)).thenReturn(PodcastEpisode(uuid = episodeUuid, publishedDate = Date()))
        whenever(bookmarkManager.add(any(), any(), any(), any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Bookmark(uuid = "new-id"))

        viewModel.load(arguments)
        val saved = CompletableDeferred<Unit>()
        viewModel.saveBookmark { _, _ -> saved.complete(Unit) }
        saved.await()

        verify(bookmarkManager).add(
            episode = any(),
            timeSecs = eq(timeSecs),
            title = eq("A great moment"),
            creationSource = any(),
            addedAt = any(),
            passage = eq("the passage"),
            passageLocation = eq(5),
            referenceTime = eq(118),
        )
        verify(bookmarkManager, never()).enrichBookmarkPassage(any())
    }

    @Test
    fun `saves without waiting for an in-flight suggestion`() = runTest {
        stubNewBookmark()
        val gate = CompletableDeferred<BookmarkSuggestion?>()
        doSuspendableAnswer { gate.await() }.whenever(bookmarkManager).suggestBookmark(episodeUuid, timeSecs)
        whenever(episodeManager.findByUuid(episodeUuid)).thenReturn(PodcastEpisode(uuid = episodeUuid, publishedDate = Date()))
        whenever(bookmarkManager.add(any(), any(), any(), any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Bookmark(uuid = "new-id"))

        viewModel.load(arguments)
        val saved = CompletableDeferred<Unit>()
        viewModel.saveBookmark { _, _ -> saved.complete(Unit) }
        saved.await()

        verify(bookmarkManager).add(
            episode = any(),
            timeSecs = eq(timeSecs),
            title = any(),
            creationSource = any(),
            addedAt = any(),
            passage = isNull(),
            passageLocation = isNull(),
            referenceTime = isNull(),
        )
        verify(bookmarkManager).enrichBookmarkPassage(any())
    }

    @Test
    fun `ignores a repeated load so the suggestion is generated once`() = runTest {
        stubNewBookmark()
        whenever(bookmarkManager.suggestBookmark(episodeUuid, timeSecs)).thenReturn(suggestion)

        viewModel.load(arguments)
        viewModel.load(arguments)

        verify(bookmarkManager).suggestBookmark(episodeUuid, timeSecs)
    }

    @Test
    fun `saves a blank title as the default`() = runTest {
        stubNewBookmark()
        whenever(bookmarkManager.suggestBookmark(episodeUuid, timeSecs)).thenReturn(null)
        whenever(episodeManager.findByUuid(episodeUuid)).thenReturn(PodcastEpisode(uuid = episodeUuid, publishedDate = Date()))
        whenever(bookmarkManager.add(any(), any(), any(), any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Bookmark(uuid = "new-id"))

        viewModel.load(arguments)
        viewModel.changeTitle(TextFieldValue("   "))
        val saved = CompletableDeferred<Unit>()
        viewModel.saveBookmark { _, _ -> saved.complete(Unit) }
        saved.await()

        verify(bookmarkManager).add(
            episode = any(),
            timeSecs = eq(timeSecs),
            title = eq("Bookmark"),
            creationSource = any(),
            addedAt = any(),
            passage = isNull(),
            passageLocation = isNull(),
            referenceTime = isNull(),
        )
    }

    @Test
    fun `caps an applied suggestion at 100 characters`() = runTest {
        viewModel.applySuggestion("a".repeat(150))

        assertEquals(100, viewModel.uiState.value.title.text.length)
    }

    @Test
    fun `captures the suggested passage into the state`() = runTest {
        stubNewBookmark()
        whenever(bookmarkManager.suggestBookmark(episodeUuid, timeSecs)).thenReturn(suggestion)

        viewModel.load(arguments)

        val state = viewModel.uiState.value
        assertEquals("the passage", state.passage)
        assertEquals(5, state.passageLocation)
    }

    @Test
    fun `saves the edited passage instead of the suggestion`() = runTest {
        stubNewBookmark()
        whenever(bookmarkManager.suggestBookmark(episodeUuid, timeSecs)).thenReturn(suggestion)
        whenever(episodeManager.findByUuid(episodeUuid)).thenReturn(PodcastEpisode(uuid = episodeUuid, publishedDate = Date()))
        whenever(bookmarkManager.add(any(), any(), any(), any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Bookmark(uuid = "new-id"))

        viewModel.load(arguments)
        viewModel.onPassageEdited("a hand-picked passage", 9)
        val saved = CompletableDeferred<Unit>()
        viewModel.saveBookmark { _, _ -> saved.complete(Unit) }
        saved.await()

        verify(bookmarkManager).add(
            episode = any(),
            timeSecs = eq(timeSecs),
            title = any(),
            creationSource = any(),
            addedAt = any(),
            passage = eq("a hand-picked passage"),
            passageLocation = eq(9),
            referenceTime = eq(118),
        )
    }

    @Test
    fun `updates the passage when saving an existing bookmark`() = runTest {
        whenever(bookmarkManager.findBookmark("existing-id")).thenReturn(Bookmark(uuid = "existing-id", title = "Kept"))

        viewModel.load(existingBookmarkArguments("existing-id"))
        viewModel.onPassageEdited("a hand-picked passage", 9)
        val saved = CompletableDeferred<Unit>()
        viewModel.saveBookmark { _, _ -> saved.complete(Unit) }
        saved.await()

        verify(bookmarkManager).updatePassage("existing-id", "a hand-picked passage", 9)
    }

    private suspend fun stubNewBookmark() {
        whenever(episodeManager.findEpisodeByUuid(episodeUuid)).thenReturn(PodcastEpisode(uuid = episodeUuid, publishedDate = Date()))
        whenever(bookmarkManager.findByEpisodeTime(any(), eq(timeSecs))).thenReturn(null)
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
