package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.content.Context
import androidx.compose.ui.text.input.TextFieldValue
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkGenerationAnalytics
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkSuggestion
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.TitleGeneration
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.shownotes.ShowNotesManager
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.automattic.eventhorizon.BookmarkEditFormDismissedEvent
import com.automattic.eventhorizon.BookmarkEditFormShownEvent
import com.automattic.eventhorizon.BookmarkEditFormSubmittedEvent
import com.automattic.eventhorizon.BookmarkTitleSuggestionTappedEvent
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SourceViewType
import java.util.Date
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
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
    private val eventSink = TestEventSink()
    private val bookmarkGenerationAnalytics = mock<BookmarkGenerationAnalytics>()
    private val viewModel = BookmarkViewModel(episodeManager, userEpisodeManager, bookmarkManager, transcriptManager, showNotesManager, EventHorizon(eventSink), bookmarkGenerationAnalytics, context)

    private val episodeUuid = "episode-id"
    private val timeSecs = 120
    private val arguments = BookmarkArguments(
        bookmarkUuid = null,
        episodeUuid = episodeUuid,
        timeSecs = timeSecs,
        podcastColors = PodcastColors.ForUserEpisode,
    )
    private val suggestion = BookmarkSuggestion(passage = "the passage", passageLocation = 5, referenceTimeSecs = 118, generation = TitleGeneration("A great moment", 0, null))

    @Before
    fun setUp() {
        FeatureFlag.setEnabled(Feature.SMART_BOOKMARKS, true)
    }

    @Test
    fun `edit form events use the source the sheet was opened from`() = runTest {
        viewModel.load(arguments.copy(source = SourceView.TRANSCRIPT))

        val event = eventSink.pollEvent()
        assertTrue(event is BookmarkEditFormShownEvent)
        assertEquals(SourceViewType.Transcript, (event as BookmarkEditFormShownEvent).source)
    }

    @Test
    fun `shown event carries the episode and podcast of the bookmark`() = runTest {
        whenever(episodeManager.findEpisodeByUuid(episodeUuid))
            .thenReturn(PodcastEpisode(uuid = episodeUuid, podcastUuid = "podcast-id", publishedDate = Date()))

        viewModel.load(arguments)

        val event = eventSink.pollEvent() as BookmarkEditFormShownEvent
        assertEquals(true, event.isNewBookmark)
        assertEquals(episodeUuid, event.episodeUuid)
        assertEquals("podcast-id", event.podcastUuid)
    }

    @Test
    fun `shown event is tracked once when load is called again`() = runTest {
        viewModel.load(arguments)
        viewModel.load(arguments)

        assertTrue(eventSink.pollEvent() is BookmarkEditFormShownEvent)
        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `shown event is tracked when the episode is missing`() = runTest {
        viewModel.load(arguments)

        val event = eventSink.pollEvent() as BookmarkEditFormShownEvent
        assertEquals(true, event.isNewBookmark)
        assertEquals(episodeUuid, event.episodeUuid)
        assertEquals(null, event.podcastUuid)
        verify(bookmarkManager, never()).suggestBookmark(any(), any())
    }

    @Test
    fun `shown event reports an existing bookmark found at the episode time as not new`() = runTest {
        val episode = PodcastEpisode(uuid = episodeUuid, podcastUuid = "podcast-id", publishedDate = Date())
        whenever(episodeManager.findEpisodeByUuid(episodeUuid)).thenReturn(episode)
        whenever(bookmarkManager.findByEpisodeTime(episode, timeSecs))
            .thenReturn(Bookmark(uuid = "existing-id", episodeUuid = episodeUuid, podcastUuid = "podcast-id", title = "Mine"))

        viewModel.load(arguments)

        val event = eventSink.pollEvent() as BookmarkEditFormShownEvent
        assertEquals(false, event.isNewBookmark)
        assertEquals("podcast-id", event.podcastUuid)
    }

    @Test
    fun `dismissing before the sheet loads tracks nothing`() = runTest {
        viewModel.onClose()

        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `dismissed event carries the episode and podcast of the bookmark`() = runTest {
        whenever(bookmarkManager.findBookmark("existing-id"))
            .thenReturn(Bookmark(uuid = "existing-id", episodeUuid = episodeUuid, podcastUuid = "podcast-id", title = "Mine"))
        viewModel.load(arguments.copy(bookmarkUuid = "existing-id"))
        eventSink.skipEvent()

        viewModel.onClose()

        val event = eventSink.pollEvent() as BookmarkEditFormDismissedEvent
        assertEquals(false, event.isNewBookmark)
        assertEquals(episodeUuid, event.episodeUuid)
        assertEquals("podcast-id", event.podcastUuid)
    }

    @Test
    fun `dismissing while the sheet is loading tracks shown before dismissed`() = runTest {
        whenever(episodeManager.findEpisodeByUuid(episodeUuid)).doSuspendableAnswer { awaitCancellation() }
        viewModel.load(arguments)

        viewModel.onClose()

        assertTrue(eventSink.pollEvent() is BookmarkEditFormShownEvent)
        assertTrue(eventSink.pollEvent() is BookmarkEditFormDismissedEvent)
        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `passage editor dismissal is ignored before the view model is loaded`() = runTest {
        viewModel.onPassageEditorDismissed("new passage")

        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `tapping the suggested title tracks it and applies the title`() = runTest {
        viewModel.load(arguments)
        eventSink.skipEvent()

        viewModel.onSuggestionTapped("A great moment")

        assertEquals("A great moment", viewModel.uiState.value.title.text)
        assertTrue(eventSink.pollEvent() is BookmarkTitleSuggestionTappedEvent)
    }

    @Test
    fun `submitting reports whether a passage was saved and changed`() = runTest {
        whenever(episodeManager.findEpisodeByUuid(episodeUuid))
            .thenReturn(PodcastEpisode(uuid = episodeUuid, podcastUuid = "podcast-id", publishedDate = Date()))
        viewModel.load(arguments)
        eventSink.skipEvent()
        viewModel.onPassageEdited("a chosen passage", 3)

        viewModel.onSubmitBookmark()

        val event = eventSink.pollEvent() as BookmarkEditFormSubmittedEvent
        assertEquals(true, event.hasPassage)
        assertEquals(true, event.passageChanged)
        assertEquals(episodeUuid, event.episodeUuid)
        assertEquals("podcast-id", event.podcastUuid)
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
        whenever(bookmarkManager.suggestTitle("a captured passage")).thenReturn(TitleGeneration("A great moment", 0, null))

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
    fun `keeps capturing the passage when the title is edited`() = runTest {
        stubNewBookmark()
        val gate = CompletableDeferred<BookmarkSuggestion?>()
        doSuspendableAnswer { gate.await() }.whenever(bookmarkManager).suggestBookmark(episodeUuid, timeSecs)

        viewModel.load(arguments)
        assertTrue(viewModel.uiState.value.isCapturingPassage)

        viewModel.changeTitle(TextFieldValue("My own title"))
        assertTrue(viewModel.uiState.value.isCapturingPassage)

        gate.complete(suggestion)

        assertFalse(viewModel.uiState.value.isCapturingPassage)
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

        verify(bookmarkManager).updatePassage("existing-id", "a hand-picked passage", 9, null)
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
