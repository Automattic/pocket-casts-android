package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import androidx.media3.common.text.Cue
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.SubtitleParser
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.TranscriptError
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel.UiState
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.TranscriptsManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.UrlUtil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class TranscriptViewModelTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val transcriptsManager: TranscriptsManager = mock()
    private val playbackManager: PlaybackManager = mock()
    private val urlUtil: UrlUtil = mock()
    private val subtitleParserFactory: SubtitleParser.Factory = mock()
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
    fun `given transcript view is not open, when transcript load invoked, then transcript is not parsed and loaded`() = runTest {
        whenever(transcriptsManager.observerTranscriptForEpisode(any())).thenReturn(flowOf(transcript))
        whenever(subtitleParserFactory.supportsFormat(any())).thenReturn(true)
        initViewModel()

        viewModel.parseAndLoadTranscript(isTranscriptViewOpen = false)

        viewModel.uiState.test {
            verifyNoInteractions(subtitleParserFactory)
            assertEquals(transcript, (awaitItem() as UiState.TranscriptFound).transcript)
        }
    }

    @Test
    fun `given transcript view is open, when transcript load invoked, then transcript is parsed`() = runTest {
        whenever(transcriptsManager.observerTranscriptForEpisode(any())).thenReturn(flowOf(transcript))
        whenever(subtitleParserFactory.supportsFormat(any())).thenReturn(true)
        whenever(urlUtil.contentBytes(anyOrNull())).thenReturn(byteArrayOf())
        val parser = mock<SubtitleParser>()
        whenever(subtitleParserFactory.create(anyOrNull())).thenReturn(parser)

        initViewModel()

        viewModel.parseAndLoadTranscript(isTranscriptViewOpen = true)

        viewModel.uiState.test {
            verify(parser).parse(any(), any(), any())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given transcript is supported, when transcript load invoked, then loaded state is returned`() = runTest {
        whenever(transcriptsManager.observerTranscriptForEpisode(any())).thenReturn(flowOf(transcript))
        whenever(subtitleParserFactory.supportsFormat(any())).thenReturn(true)
        initViewModel()

        viewModel.parseAndLoadTranscript(isTranscriptViewOpen = true)

        viewModel.uiState.test {
            assertEquals(transcript, (awaitItem() as UiState.TranscriptLoaded).transcript)
        }
    }

    @Test
    fun `given transcript is not supported, when transcript load invoked, then NotSupported error is returned`() = runTest {
        whenever(transcriptsManager.observerTranscriptForEpisode(any())).thenReturn(flowOf(transcript))
        whenever(subtitleParserFactory.supportsFormat(any())).thenReturn(false)
        initViewModel()

        viewModel.parseAndLoadTranscript(isTranscriptViewOpen = true)

        viewModel.uiState.test {
            assertTrue((awaitItem() as UiState.Error).error is TranscriptError.NotSupported)
        }
    }

    @Test
    fun `given transcript type supported but content not valid, then FailedToLoad error is returned`() = runTest {
        whenever(transcriptsManager.observerTranscriptForEpisode(any())).thenReturn(flowOf(transcript))
        whenever(subtitleParserFactory.supportsFormat(any())).thenReturn(true)
        whenever(urlUtil.contentBytes(any())).thenThrow(RuntimeException())
        initViewModel()

        viewModel.parseAndLoadTranscript(isTranscriptViewOpen = true)

        viewModel.uiState.test {
            assertTrue((awaitItem() as UiState.Error).error is TranscriptError.FailedToLoad)
        }
    }

    @Test
    fun `speaker is trimmed from cue text`() = runTest {
        whenever(transcriptsManager.observerTranscriptForEpisode(any())).thenReturn(flowOf(transcript))
        whenever(subtitleParserFactory.supportsFormat(any())).thenReturn(true)
        initViewModel()
        val cuesWithTiming = CuesWithTiming(listOf(Cue.Builder().setText("Speaker 11: Text").build()), 0L, 0L)

        val result = viewModel.modifiedCues(cuesWithTiming)

        assertTrue(result[0].text == "Text")
    }

    @Test
    fun `new line added after period, exclamation mark, or question mark at end of cue text`() = runTest {
        whenever(transcriptsManager.observerTranscriptForEpisode(any())).thenReturn(flowOf(transcript))
        whenever(subtitleParserFactory.supportsFormat(any())).thenReturn(true)
        initViewModel()
        val cuesWithTiming = CuesWithTiming(
            listOf(
                Cue.Builder().setText("Text.").build(),
                Cue.Builder().setText("Text!").build(),
                Cue.Builder().setText("Text?").build(),
            ),
            0L,
            0L,
        )

        val result = viewModel.modifiedCues(cuesWithTiming)

        assertTrue(result[0].text == "Text.\n\n")
        assertTrue(result[1].text == "Text!\n\n")
        assertTrue(result[2].text == "Text?\n\n")
    }

    @Test
    fun `new line not added after period, exclamation mark, or question mark in middle of the cue text`() = runTest {
        whenever(transcriptsManager.observerTranscriptForEpisode(any())).thenReturn(flowOf(transcript))
        whenever(subtitleParserFactory.supportsFormat(any())).thenReturn(true)
        initViewModel()
        val cuesWithTiming = CuesWithTiming(listOf(Cue.Builder().setText("Text1.!? Text2").build()), 0L, 0L)

        val result = viewModel.modifiedCues(cuesWithTiming)

        assertTrue(result[0].text == "Text1.!? Text2")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun initViewModel() {
        whenever(playbackManager.playbackStateFlow).thenReturn(playbackStateFlow)
        viewModel = TranscriptViewModel(
            transcriptsManager = transcriptsManager,
            playbackManager = playbackManager,
            urlUtil = urlUtil,
            subtitleParserFactory = subtitleParserFactory,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }
}
