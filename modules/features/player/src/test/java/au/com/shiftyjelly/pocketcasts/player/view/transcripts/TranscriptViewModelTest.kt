package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.TranscriptError
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.UiState
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.TranscriptsManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.exception.EmptyDataException
import au.com.shiftyjelly.pocketcasts.utils.exception.NoNetworkException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TranscriptViewModelTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val transcriptsManager: TranscriptsManager = mock()
    private val playbackManager: PlaybackManager = mock()
    private val transcript: Transcript = Transcript("episode_id", "url", "type")
    private val playbackStateFlow = MutableStateFlow(PlaybackState(podcast = Podcast("podcast_id"), episodeUuid = "episode_id"))
    private lateinit var viewModel: TranscriptViewModel

    @Test
    fun `given no transcript available, then Empty state is returned`() = runTest {
        whenever(transcriptsManager.observerTranscriptForEpisode(any())).thenReturn(flowOf(null))

        initViewModel()

        viewModel.uiState.test {
            assertTrue((awaitItem() is UiState.Empty))
        }
    }

    @Test
    fun `given transcript is available, then transcript found state is returned`() = runTest {
        whenever(transcriptsManager.observerTranscriptForEpisode(any())).thenReturn(flowOf(transcript))

        initViewModel()

        viewModel.uiState.test {
            assertEquals(transcript, (awaitItem() as UiState.TranscriptFound).transcript)
        }
    }

    @Test
    fun `given transcript view is open, when transcript load invoked, then transcript is loaded`() = runTest {
        whenever(transcriptsManager.observerTranscriptForEpisode(any())).thenReturn(flowOf(transcript))
        initViewModel()

        viewModel.parseAndLoadTranscript(isTranscriptViewOpen = true)

        viewModel.uiState.test {
            assertTrue(awaitItem() is UiState.TranscriptLoaded)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given transcript is supported but blank, when transcript load invoked, then Empty error is returned`() = runTest {
        whenever(transcriptsManager.observerTranscriptForEpisode(any())).thenReturn(flowOf(transcript))
        initViewModel(transcriptLoadException = EmptyDataException(""))

        viewModel.parseAndLoadTranscript(isTranscriptViewOpen = true)

        viewModel.uiState.test {
            assertEquals((awaitItem() as UiState.Error).error, TranscriptError.Empty)
        }
    }

    @Test
    fun `given transcript is not supported, when transcript load invoked, then NotSupported error is returned`() = runTest {
        whenever(transcriptsManager.observerTranscriptForEpisode(any())).thenReturn(flowOf(transcript))
        initViewModel(transcriptLoadException = UnsupportedOperationException())

        viewModel.parseAndLoadTranscript(isTranscriptViewOpen = true)

        viewModel.uiState.test {
            assertTrue((awaitItem() as UiState.Error).error is TranscriptError.NotSupported)
        }
    }

    @Test
    fun `given transcript type supported but content not valid, then FailedToLoad error is returned`() = runTest {
        whenever(transcriptsManager.observerTranscriptForEpisode(any())).thenReturn(flowOf(transcript))
        initViewModel(transcriptLoadException = RuntimeException())

        viewModel.parseAndLoadTranscript(isTranscriptViewOpen = true)

        viewModel.uiState.test {
            assertTrue((awaitItem() as UiState.Error).error is TranscriptError.FailedToLoad)
        }
    }

    @Test
    fun `given error due to no internet, then NoNetwork error is returned`() = runTest {
        whenever(transcriptsManager.observerTranscriptForEpisode(any())).thenReturn(flowOf(transcript))
        initViewModel(transcriptLoadException = NoNetworkException())

        viewModel.parseAndLoadTranscript(isTranscriptViewOpen = true)

        viewModel.uiState.test {
            assertTrue((awaitItem() as UiState.Error).error is TranscriptError.NoNetwork)
        }
    }

    @Test
    fun `given force refresh, when transcript load invoked, then transcript is refreshed`() = runTest {
        whenever(transcriptsManager.observerTranscriptForEpisode(any())).thenReturn(flowOf(transcript))
        initViewModel()

        viewModel.parseAndLoadTranscript(isTranscriptViewOpen = true, pulledToRefresh = true)

        verify(transcriptsManager).loadTranscriptCuesInfo(transcript, forceRefresh = true)
    }

    @Test
    fun `given no force refresh, when transcript load invoked, then transcript is not refreshed`() = runTest {
        whenever(transcriptsManager.observerTranscriptForEpisode(any())).thenReturn(flowOf(transcript))
        initViewModel()

        viewModel.parseAndLoadTranscript(isTranscriptViewOpen = true, pulledToRefresh = false)

        verify(transcriptsManager).loadTranscriptCuesInfo(transcript, forceRefresh = false)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun initViewModel(
        content: String? = null,
        transcriptLoadException: Exception? = null,
    ) = runTest {
        whenever(playbackManager.playbackStateFlow).thenReturn(playbackStateFlow)
        if (transcriptLoadException != null) {
            given(transcriptsManager.loadTranscriptCuesInfo(anyOrNull(), anyOrNull(), anyOrNull())).willAnswer { throw transcriptLoadException }
        } else {
            val response = mock<ResponseBody>()
            if (content != null) {
                whenever(response.string()).thenReturn(content)
            } else {
                whenever(response.bytes()).thenReturn(byteArrayOf())
            }
            whenever(transcriptsManager.loadTranscriptCuesInfo(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(emptyList())
        }

        viewModel = TranscriptViewModel(
            transcriptsManager = transcriptsManager,
            playbackManager = playbackManager,
            ioDispatcher = UnconfinedTestDispatcher(),
            analyticsTracker = mock(),
        )
    }
}
