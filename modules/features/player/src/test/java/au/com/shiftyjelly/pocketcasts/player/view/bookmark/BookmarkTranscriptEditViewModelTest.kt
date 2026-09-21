package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptType
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
    private lateinit var transcriptManager: TranscriptManager

    @Mock
    private lateinit var showNotesManager: ShowNotesManager

    private val episodeUuid = "episode-uuid"
    private val podcastUuid = "podcast-uuid"
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

    private val timedTranscript = Transcript.Text(
        entries = listOf(
            TranscriptEntry.Text(firstSentence, startTimeMs = 0),
            TranscriptEntry.Text(secondSentence, startTimeMs = 10_000),
        ),
        type = TranscriptType.Vtt,
        url = "https://example.com/transcript.vtt",
        isGenerated = true,
        episodeUuid = episodeUuid,
        podcastUuid = podcastUuid,
    )

    @Test
    fun `loads and relocates the stored passage`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(transcript)

        val viewModel = viewModel().apply { load(arguments(firstSentence, 0)) }

        val state = viewModel.uiState.value
        assertTrue(state is BookmarkTranscriptEditViewModel.UiState.Loaded)
        state as BookmarkTranscriptEditViewModel.UiState.Loaded
        assertEquals(firstSentence, state.transcript.displaySubstring(state.passage!!))
        verify(showNotesManager).loadShowNotes(podcastUuid, episodeUuid)
    }

    @Test
    fun `can save when the stored passage relocates`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(transcript)

        val viewModel = viewModel().apply { load(arguments(firstSentence, 0)) }

        assertTrue((viewModel.uiState.value as BookmarkTranscriptEditViewModel.UiState.Loaded).canSave)
    }

    @Test
    fun `cannot save when the stored passage is absent from the transcript`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(transcript)

        val viewModel = viewModel().apply { load(arguments("a passage no transcript would ever contain", null)) }

        val loaded = viewModel.uiState.value as BookmarkTranscriptEditViewModel.UiState.Loaded
        assertNull(loaded.passage)
        assertFalse(loaded.canSave)
    }

    @Test
    fun `is not available when there is no passage`() = runTest {
        val viewModel = viewModel().apply { load(arguments(null, null)) }

        assertEquals(BookmarkTranscriptEditViewModel.UiState.NotAvailable, viewModel.uiState.value)
    }

    @Test
    fun `is not available when the transcript is missing`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(null)

        val viewModel = viewModel().apply { load(arguments(firstSentence, 0)) }

        assertEquals(BookmarkTranscriptEditViewModel.UiState.NotAvailable, viewModel.uiState.value)
    }

    @Test
    fun `save returns the re-selected passage`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(transcript)
        val viewModel = viewModel().apply { load(arguments(firstSentence, 0)) }
        val loaded = viewModel.uiState.value as BookmarkTranscriptEditViewModel.UiState.Loaded

        viewModel.onPassageChange(loaded.transcript.sentenceDisplaySpan(loaded.transcript.displayText.indexOf(secondSentence)))
        var savedPassage: String? = null
        var savedLocation: Int? = null
        viewModel.save { passage, location ->
            savedPassage = passage
            savedLocation = location
        }

        assertEquals(secondSentence, savedPassage)
        assertEquals(firstSentence.length + 1, savedLocation)
    }

    @Test
    fun `computes the reference offset at the bookmark time`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(timedTranscript)

        val viewModel = viewModel().apply { load(arguments(secondSentence, firstSentence.length + 1, referenceTime = 10)) }

        val state = viewModel.uiState.value as BookmarkTranscriptEditViewModel.UiState.Loaded
        assertEquals(state.transcript.displayText.indexOf(secondSentence), state.referenceOffset)
    }

    @Test
    fun `keeps the reference offset inside the passage`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(timedTranscript)

        val viewModel = viewModel().apply { load(arguments(firstSentence, 0, referenceTime = 10)) }

        val state = viewModel.uiState.value as BookmarkTranscriptEditViewModel.UiState.Loaded
        assertEquals(state.passage!!.end - 1, state.referenceOffset)
    }

    @Test
    fun `marks the passage start without a bookmark reference time`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(transcript)

        val viewModel = viewModel().apply { load(arguments(firstSentence, 0)) }

        val state = viewModel.uiState.value as BookmarkTranscriptEditViewModel.UiState.Loaded
        assertEquals(state.passage!!.start, state.referenceOffset)
    }

    @Test
    fun `keeps the reference offset inside the re-selected passage`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(transcript)
        val viewModel = viewModel().apply { load(arguments(firstSentence, 0)) }
        val loaded = viewModel.uiState.value as BookmarkTranscriptEditViewModel.UiState.Loaded

        viewModel.onPassageChange(loaded.transcript.sentenceDisplaySpan(loaded.transcript.displayText.indexOf(secondSentence)))

        val state = viewModel.uiState.value as BookmarkTranscriptEditViewModel.UiState.Loaded
        assertEquals(state.passage!!.start, state.referenceOffset)
    }

    private fun viewModel() = BookmarkTranscriptEditViewModel(transcriptManager, showNotesManager)

    private fun arguments(passage: String?, passageLocation: Int?, referenceTime: Int? = null) = BookmarkTranscriptEditArguments(
        episodeUuid = episodeUuid,
        podcastUuid = podcastUuid,
        passage = passage,
        passageLocation = passageLocation,
        referenceTime = referenceTime,
        podcastColors = PodcastColors.ForUserEpisode,
    )
}
