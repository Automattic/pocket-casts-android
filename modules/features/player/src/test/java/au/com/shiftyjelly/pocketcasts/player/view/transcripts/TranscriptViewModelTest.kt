package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import androidx.media3.common.text.Cue
import androidx.media3.extractor.text.CuesWithTiming
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptCuesInfo
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.TranscriptError
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.TranscriptState
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.UiState
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.TranscriptsManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.exception.EmptyDataException
import au.com.shiftyjelly.pocketcasts.utils.exception.NoNetworkException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
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
    private val subscriptionManager: SubscriptionManager = mock()
    private val podcastId = "podcast_id"
    private val transcript: Transcript = Transcript("episode_id", "url", "type", isGenerated = false)
    private val playbackState = PlaybackState(podcast = Podcast("podcast_id"), episodeUuid = "episode_id")
    private val playbackStateFlow = MutableSharedFlow<PlaybackState>()
    private val subscriptionTierFlow = MutableSharedFlow<SubscriptionTier>()
    private lateinit var viewModel: TranscriptViewModel

    @Before
    fun setup() = runBlocking {
        whenever(playbackManager.playbackStateFlow).thenReturn(playbackStateFlow)
        whenever(subscriptionManager.subscriptionTier()).thenReturn(subscriptionTierFlow)

        viewModel = TranscriptViewModel(
            transcriptsManager = transcriptsManager,
            playbackManager = playbackManager,
            analyticsTracker = mock(),
            subscriptionManager = subscriptionManager,
        )
    }

    @Test
    fun `given no transcript available, then Empty state is returned`() = runTest {
        viewModel.uiState.test {
            assertEquals(UiState.Empty, awaitItem())
        }
    }

    @Test
    fun `given transcript is available, then transcript found state is returned`() = runTest {
        whenever(transcriptsManager.observeTranscriptForEpisode(any())).thenReturn(flowOf(transcript))

        viewModel.uiState.test {
            skipItems(1)

            playbackStateFlow.emit(playbackState)
            assertEquals(transcript, (awaitItem().transcriptState as TranscriptState.Found).transcript)
        }
    }

    @Test
    fun `given transcript view is open, when transcript load invoked, then transcript is loaded`() = runTest {
        whenever(transcriptsManager.observeTranscriptForEpisode(any())).thenReturn(flowOf(transcript))

        viewModel.uiState.test {
            skipItems(1)

            playbackStateFlow.emit(playbackState)
            skipItems(1)

            prepareCues()
            viewModel.parseAndLoadTranscript()
            assertTrue(awaitItem().transcriptState is TranscriptState.Loaded)
        }
    }

    @Test
    fun `given transcript is supported but blank, when transcript load invoked, then Empty error is returned`() = runTest {
        whenever(transcriptsManager.observeTranscriptForEpisode(any())).thenReturn(flowOf(transcript))

        viewModel.uiState.test {
            skipItems(1)

            playbackStateFlow.emit(playbackState)
            skipItems(1)

            prepareCuesError(EmptyDataException(""))
            viewModel.parseAndLoadTranscript()
            assertTrue((awaitItem().transcriptState as TranscriptState.Error).error is TranscriptError.Empty)
        }
    }

    @Test
    fun `given transcript is not supported, when transcript load invoked, then NotSupported error is returned`() = runTest {
        whenever(transcriptsManager.observeTranscriptForEpisode(any())).thenReturn(flowOf(transcript))

        viewModel.uiState.test {
            skipItems(1)

            playbackStateFlow.emit(playbackState)
            skipItems(1)

            prepareCuesError(UnsupportedOperationException())
            viewModel.parseAndLoadTranscript()
            assertTrue((awaitItem().transcriptState as TranscriptState.Error).error is TranscriptError.NotSupported)
        }
    }

    @Test
    fun `given transcript type supported but content not valid, then FailedToLoad error is returned`() = runTest {
        whenever(transcriptsManager.observeTranscriptForEpisode(any())).thenReturn(flowOf(transcript))

        viewModel.uiState.test {
            skipItems(1)

            playbackStateFlow.emit(playbackState)
            skipItems(1)

            prepareCuesError(RuntimeException())
            viewModel.parseAndLoadTranscript()
            assertTrue((awaitItem().transcriptState as TranscriptState.Error).error is TranscriptError.FailedToLoad)
        }
    }

    @Test
    fun `given error due to no internet, then NoNetwork error is returned`() = runTest {
        whenever(transcriptsManager.observeTranscriptForEpisode(any())).thenReturn(flowOf(transcript))

        viewModel.uiState.test {
            skipItems(1)

            playbackStateFlow.emit(playbackState)
            skipItems(1)

            prepareCuesError(NoNetworkException())
            viewModel.parseAndLoadTranscript()
            assertTrue((awaitItem().transcriptState as TranscriptState.Error).error is TranscriptError.NoNetwork)
        }
    }

    @Test
    fun `given force refresh, when transcript load invoked, then transcript is refreshed`() = runTest {
        whenever(transcriptsManager.observeTranscriptForEpisode(any())).thenReturn(flowOf(transcript))

        viewModel.uiState.test {
            playbackStateFlow.emit(playbackState)

            viewModel.parseAndLoadTranscript(pulledToRefresh = true)
            verify(transcriptsManager).loadTranscriptCuesInfo(podcastId, transcript, forceRefresh = true)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given no force refresh, when transcript load invoked, then transcript is not refreshed`() = runTest {
        whenever(transcriptsManager.observeTranscriptForEpisode(any())).thenReturn(flowOf(transcript))

        viewModel.uiState.test {
            playbackStateFlow.emit(playbackState)

            viewModel.parseAndLoadTranscript(pulledToRefresh = false)
            verify(transcriptsManager).loadTranscriptCuesInfo(podcastId, transcript, forceRefresh = false)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given html transcript with javascript, when transcript load invoked, then transcript is shown in webview`() = runTest {
        whenever(transcriptsManager.observeTranscriptForEpisode(any())).thenReturn(flowOf(transcript.copy(type = "text/html")))

        viewModel.uiState.test {
            skipItems(1)

            playbackStateFlow.emit(playbackState)
            skipItems(1)

            prepareCues("<html><script type=\"text/javascript\"></html>")
            viewModel.parseAndLoadTranscript()
            assertTrue((awaitItem().transcriptState as TranscriptState.Loaded).showAsWebPage)
        }
    }

    @Test
    fun `given html transcript without javascript, when transcript load invoked, then transcript is not shown in webview`() = runTest {
        whenever(transcriptsManager.observeTranscriptForEpisode(any())).thenReturn(flowOf(transcript.copy(type = "text/html")))

        viewModel.uiState.test {
            skipItems(1)

            playbackStateFlow.emit(playbackState)
            skipItems(1)

            prepareCues("<html></html>")
            viewModel.parseAndLoadTranscript()
            assertFalse((awaitItem().transcriptState as TranscriptState.Loaded).showAsWebPage)
        }
    }

    @Test
    fun `given html transcript with javascript, when transcript load invoked, then search is not shown`() = runTest {
        whenever(transcriptsManager.observeTranscriptForEpisode(any())).thenReturn(flowOf(transcript.copy(type = "text/html")))

        viewModel.uiState.test {
            skipItems(1)

            playbackStateFlow.emit(playbackState)
            skipItems(1)

            prepareCues("<html><script type=\"text/javascript\"></html>")
            viewModel.parseAndLoadTranscript()
            assertFalse(awaitItem().showSearch)
        }
    }

    @Test
    fun `given html transcript without javascript, when transcript load invoked, then search is shown`() = runTest {
        whenever(transcriptsManager.observeTranscriptForEpisode(any())).thenReturn(flowOf(transcript.copy(type = "text/html")))

        viewModel.uiState.test {
            skipItems(1)

            playbackStateFlow.emit(playbackState)
            skipItems(1)

            prepareCues("<html></html>")
            viewModel.parseAndLoadTranscript()
            assertTrue(awaitItem().showSearch)
        }
    }

    @Test
    fun `given generated transcript, when user is not subscribed, then show paywall`() = runTest {
        whenever(transcriptsManager.observeTranscriptForEpisode(any())).thenReturn(flowOf(transcript.copy(isGenerated = true)))

        viewModel.uiState.test {
            skipItems(1)

            playbackStateFlow.emit(playbackState)
            skipItems(1)

            prepareCues("Hello")
            viewModel.parseAndLoadTranscript()
            assertTrue(awaitItem().showPaywall)
        }
    }

    @Test
    fun `given generated transcript, when user is Plus, then do not show paywall`() = runTest {
        whenever(transcriptsManager.observeTranscriptForEpisode(any())).thenReturn(flowOf(transcript.copy(isGenerated = true)))

        viewModel.uiState.test {
            skipItems(1)

            playbackStateFlow.emit(playbackState)
            skipItems(1)

            subscriptionTierFlow.emit(SubscriptionTier.PLUS)
            assertFalse(awaitItem().showPaywall)

            prepareCues("Hello")
            viewModel.parseAndLoadTranscript()
            assertFalse(awaitItem().showPaywall)
        }
    }

    @Test
    fun `given generated transcript, when user is Patron, then do not show paywall`() = runTest {
        whenever(transcriptsManager.observeTranscriptForEpisode(any())).thenReturn(flowOf(transcript.copy(isGenerated = true)))

        viewModel.uiState.test {
            skipItems(1)

            playbackStateFlow.emit(playbackState)
            skipItems(1)

            subscriptionTierFlow.emit(SubscriptionTier.PATRON)
            assertFalse(awaitItem().showPaywall)

            prepareCues("Hello")
            viewModel.parseAndLoadTranscript()
            assertFalse(awaitItem().showPaywall)
        }
    }

    @Test
    fun `given generated transcript, when user is not subscribed and there is no transcript content, then do not show paywall`() = runTest {
        whenever(transcriptsManager.observeTranscriptForEpisode(any())).thenReturn(flowOf(transcript.copy(isGenerated = true)))

        viewModel.uiState.test {
            skipItems(1)

            playbackStateFlow.emit(playbackState)
            skipItems(1)

            prepareCues(content = null)
            viewModel.parseAndLoadTranscript()
            assertFalse(awaitItem().showPaywall)
        }
    }

    private suspend fun prepareCues(content: String? = null) {
        val response = mock<ResponseBody>()
        if (content != null) {
            whenever(response.string()).thenReturn(content)
        } else {
            whenever(response.bytes()).thenReturn(byteArrayOf())
        }
        val cuesInfo = if (content == null) {
            emptyList()
        } else {
            listOf(TranscriptCuesInfo(CuesWithTiming(listOf(Cue.Builder().setText(content).build()), 0, 0), null))
        }
        whenever(transcriptsManager.loadTranscriptCuesInfo(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(cuesInfo)
    }

    private suspend fun prepareCuesError(exception: Exception) {
        given(transcriptsManager.loadTranscriptCuesInfo(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).willAnswer { throw exception }
    }
}
