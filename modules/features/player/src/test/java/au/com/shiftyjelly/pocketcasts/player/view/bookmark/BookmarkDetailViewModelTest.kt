package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptType
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
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarkDetailViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val transcriptManager = mock<TranscriptManager>()
    private val viewModel = BookmarkDetailViewModel(transcriptManager)

    private val episodeUuid = "episode-id"
    private val firstSentence = "that's the thing about selective admissions."
    private val transcript = Transcript.Text(
        entries = listOf(TranscriptEntry.Text(firstSentence)),
        type = TranscriptType.Vtt,
        url = "https://example.com/transcript.vtt",
        isGenerated = true,
        episodeUuid = episodeUuid,
        podcastUuid = "podcast-id",
    )

    @Before
    fun setUp() {
        FeatureFlag.setEnabled(Feature.SMART_BOOKMARKS, true)
    }

    @Test
    fun `loads and relocates the passage`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(transcript)

        viewModel.load(episodeUuid, passage = firstSentence, passageLocation = 0)

        val state = viewModel.transcriptState.value
        assertTrue(state is BookmarkDetailViewModel.TranscriptState.Loaded)
        state as BookmarkDetailViewModel.TranscriptState.Loaded
        assertEquals(firstSentence, state.transcript.displaySubstring(state.passage!!))
    }

    @Test
    fun `is unavailable when the transcript is missing`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(null)

        viewModel.load(episodeUuid, passage = firstSentence, passageLocation = 0)

        assertEquals(BookmarkDetailViewModel.TranscriptState.Unavailable, viewModel.transcriptState.value)
    }

    @Test
    fun `is none when there is no passage`() = runTest {
        viewModel.load(episodeUuid, passage = null, passageLocation = null)

        assertEquals(BookmarkDetailViewModel.TranscriptState.None, viewModel.transcriptState.value)
    }

    @Test
    fun `is none when the flag is off`() = runTest {
        FeatureFlag.setEnabled(Feature.SMART_BOOKMARKS, false)

        viewModel.load(episodeUuid, passage = firstSentence, passageLocation = 0)

        assertEquals(BookmarkDetailViewModel.TranscriptState.None, viewModel.transcriptState.value)
    }
}
