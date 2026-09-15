package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptType
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.shownotes.ShowNotesManager
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarkDetailViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val bookmarkManager = mock<BookmarkManager>()
    private val transcriptManager = mock<TranscriptManager>()
    private val showNotesManager = mock<ShowNotesManager>()
    private val viewModel = BookmarkDetailViewModel(bookmarkManager, transcriptManager, showNotesManager)

    private val bookmarkUuid = "bookmark-id"
    private val episodeUuid = "episode-id"
    private val podcastUuid = "podcast-id"
    private val firstSentence = "that's the thing about selective admissions."
    private val secondSentence = "The difference between the kid who gets in and the kid who doesn't is often basically noise."
    private val transcript = Transcript.Text(
        entries = listOf(TranscriptEntry.Text(firstSentence), TranscriptEntry.Text(secondSentence)),
        type = TranscriptType.Vtt,
        url = "https://example.com/transcript.vtt",
        isGenerated = true,
        episodeUuid = episodeUuid,
        podcastUuid = podcastUuid,
    )

    @Before
    fun setUp() {
        FeatureFlag.setEnabled(Feature.SMART_BOOKMARKS, true)
    }

    @Test
    fun `loads and relocates the passage`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(transcript)

        load(passage = firstSentence, passageLocation = 0)

        val state = viewModel.uiState.value.transcriptState
        assertTrue(state is BookmarkDetailViewModel.TranscriptState.Loaded)
        state as BookmarkDetailViewModel.TranscriptState.Loaded
        assertEquals(firstSentence, state.transcript.displaySubstring(state.passage!!))
    }

    @Test
    fun `is unavailable when the transcript is missing`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(null)

        load(passage = firstSentence, passageLocation = 0)

        assertEquals(BookmarkDetailViewModel.TranscriptState.Unavailable, viewModel.uiState.value.transcriptState)
    }

    @Test
    fun `is none when there is no passage`() = runTest {
        load(passage = null, passageLocation = null)

        assertEquals(BookmarkDetailViewModel.TranscriptState.None, viewModel.uiState.value.transcriptState)
    }

    @Test
    fun `is none when the flag is off`() = runTest {
        FeatureFlag.setEnabled(Feature.SMART_BOOKMARKS, false)

        load(passage = firstSentence, passageLocation = 0)

        assertEquals(BookmarkDetailViewModel.TranscriptState.None, viewModel.uiState.value.transcriptState)
    }

    @Test
    fun `loads show notes before resolving the transcript`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(transcript)

        load(passage = firstSentence, passageLocation = 0)

        verifyBlocking(showNotesManager) { loadShowNotes(podcastUuid, episodeUuid) }
    }

    @Test
    fun `is unavailable when the passage cannot be relocated`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(transcript)

        load(passage = "not in the transcript", passageLocation = null)

        assertEquals(BookmarkDetailViewModel.TranscriptState.Unavailable, viewModel.uiState.value.transcriptState)
    }

    @Test
    fun `does not reload after the first load`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(transcript)

        load(passage = firstSentence, passageLocation = 0)
        load(passage = firstSentence, passageLocation = 0)

        verifyBlocking(transcriptManager, times(1)) { loadGeneratedTranscript(episodeUuid) }
    }

    @Test
    fun `refresh re-reads the bookmark and relocates the passage`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(transcript)
        load(passage = firstSentence, passageLocation = 0)
        whenever(bookmarkManager.findBookmark(bookmarkUuid)).thenReturn(
            Bookmark(
                uuid = bookmarkUuid,
                podcastUuid = podcastUuid,
                episodeUuid = episodeUuid,
                title = "Renamed",
                passage = secondSentence,
                passageLocation = firstSentence.length + 1,
            ),
        )

        viewModel.refresh()

        val state = viewModel.uiState.value
        assertEquals("Renamed", state.title)
        val transcriptState = state.transcriptState
        assertTrue(transcriptState is BookmarkDetailViewModel.TranscriptState.Loaded)
        transcriptState as BookmarkDetailViewModel.TranscriptState.Loaded
        assertEquals(secondSentence, transcriptState.transcript.displaySubstring(transcriptState.passage!!))
    }

    private fun load(passage: String?, passageLocation: Int?) {
        viewModel.load(
            bookmarkUuid = bookmarkUuid,
            title = "Title",
            episodeUuid = episodeUuid,
            podcastUuid = podcastUuid,
            passage = passage,
            passageLocation = passageLocation,
        )
    }
}
