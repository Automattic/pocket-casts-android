package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptType
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.shownotes.ShowNotesManager
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class BookmarkTranscriptEditViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var bookmarkManager: BookmarkManager

    @Mock
    private lateinit var transcriptManager: TranscriptManager

    @Mock
    private lateinit var showNotesManager: ShowNotesManager

    private val bookmarkUuid = "bookmark-uuid"
    private val episodeUuid = "episode-uuid"
    private val podcastUuid = "podcast-uuid"
    private val firstSentence = "that's the thing about selective admissions."
    private val secondSentence = "The difference between the kid who gets in and the kid who doesn't is often basically noise."

    private val arguments = BookmarkTranscriptEditArguments(
        bookmarkUuid = bookmarkUuid,
        episodeUuid = episodeUuid,
        podcastColors = PodcastColors.ForUserEpisode,
    )

    private val transcript = Transcript.Text(
        entries = listOf(TranscriptEntry.Text(firstSentence), TranscriptEntry.Text(secondSentence)),
        type = TranscriptType.Vtt,
        url = "https://example.com/transcript.vtt",
        isGenerated = true,
        episodeUuid = episodeUuid,
        podcastUuid = "podcast-uuid",
    )

    @Test
    fun `loads and relocates the stored passage`() = runTest {
        whenever(bookmarkManager.findBookmark(bookmarkUuid)).thenReturn(bookmark(passage = firstSentence, location = 0))
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(transcript)

        val viewModel = viewModel().apply { load(arguments) }

        val state = viewModel.uiState.value
        assertTrue(state is BookmarkTranscriptEditViewModel.UiState.Loaded)
        state as BookmarkTranscriptEditViewModel.UiState.Loaded
        assertEquals(firstSentence, state.transcript.displaySubstring(state.passage!!))
        verify(showNotesManager).loadShowNotes(podcastUuid, episodeUuid)
    }

    @Test
    fun `can save when the stored passage relocates`() = runTest {
        whenever(bookmarkManager.findBookmark(bookmarkUuid)).thenReturn(bookmark(passage = firstSentence, location = 0))
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(transcript)

        val viewModel = viewModel().apply { load(arguments) }

        assertTrue((viewModel.uiState.value as BookmarkTranscriptEditViewModel.UiState.Loaded).canSave)
    }

    @Test
    fun `cannot save when the stored passage is absent from the transcript`() = runTest {
        whenever(bookmarkManager.findBookmark(bookmarkUuid)).thenReturn(bookmark(passage = "a passage no transcript would ever contain", location = null))
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(transcript)

        val viewModel = viewModel().apply { load(arguments) }

        val loaded = viewModel.uiState.value as BookmarkTranscriptEditViewModel.UiState.Loaded
        assertNull(loaded.passage)
        assertFalse(loaded.canSave)
    }

    @Test
    fun `is not available when the bookmark has no passage`() = runTest {
        whenever(bookmarkManager.findBookmark(bookmarkUuid)).thenReturn(bookmark(passage = null, location = null))

        val viewModel = viewModel().apply { load(arguments) }

        assertEquals(BookmarkTranscriptEditViewModel.UiState.NotAvailable, viewModel.uiState.value)
    }

    @Test
    fun `is not available when the transcript is missing`() = runTest {
        whenever(bookmarkManager.findBookmark(bookmarkUuid)).thenReturn(bookmark(passage = firstSentence, location = 0))
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(null)

        val viewModel = viewModel().apply { load(arguments) }

        assertEquals(BookmarkTranscriptEditViewModel.UiState.NotAvailable, viewModel.uiState.value)
    }

    @Test
    fun `save persists the re-selected passage`() = runTest {
        whenever(bookmarkManager.findBookmark(bookmarkUuid)).thenReturn(bookmark(passage = firstSentence, location = 0))
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(transcript)
        val viewModel = viewModel().apply { load(arguments) }
        val loaded = viewModel.uiState.value as BookmarkTranscriptEditViewModel.UiState.Loaded

        viewModel.onPassageChange(loaded.transcript.sentenceDisplaySpan(loaded.transcript.displayText.indexOf(secondSentence)))
        var saved = false
        viewModel.save { saved = true }

        assertTrue(saved)
        verify(bookmarkManager).updatePassage(bookmarkUuid, secondSentence, firstSentence.length + 1)
    }

    private fun viewModel() = BookmarkTranscriptEditViewModel(bookmarkManager, transcriptManager, showNotesManager)

    private fun bookmark(passage: String?, location: Int?) = Bookmark(
        uuid = bookmarkUuid,
        podcastUuid = podcastUuid,
        episodeUuid = episodeUuid,
        passage = passage,
        passageLocation = location,
    )
}
