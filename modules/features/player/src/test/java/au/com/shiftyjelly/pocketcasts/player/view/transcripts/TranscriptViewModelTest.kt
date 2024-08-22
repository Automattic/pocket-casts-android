package au.com.shiftyjelly.pocketcasts.player.view.transcripts

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
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class TranscriptViewModelTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val transcriptsManager: TranscriptsManager = mock()
    private val playbackManager: PlaybackManager = mock()
    private val subtitleParserFactory: SubtitleParser.Factory = mock()
    private val transcriptJsonConverter: TranscriptJsonConverter = mock()
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
        val parser = mock<SubtitleParser>()
        doNothing().whenever(parser).parse(anyOrNull(), eq(SubtitleParser.OutputOptions.allCues()), anyOrNull())
        whenever(subtitleParserFactory.create(anyOrNull())).thenReturn(parser)
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
        initViewModel(transcriptLoadException = RuntimeException())

        viewModel.parseAndLoadTranscript(isTranscriptViewOpen = true)

        viewModel.uiState.test {
            assertTrue((awaitItem() as UiState.Error).error is TranscriptError.FailedToLoad)
        }
    }

    @Test
    fun `given error due to no internet, then NoNetwork error is returned`() = runTest {
        whenever(transcriptsManager.observerTranscriptForEpisode(any())).thenReturn(flowOf(transcript))
        whenever(subtitleParserFactory.supportsFormat(any())).thenReturn(true)
        initViewModel(transcriptLoadException = NoNetworkException())

        viewModel.parseAndLoadTranscript(isTranscriptViewOpen = true)

        viewModel.uiState.test {
            assertTrue((awaitItem() as UiState.Error).error is TranscriptError.NoNetwork)
        }
    }

    @Test
    fun `given mimetype html, when transcript load invoked, then transcript is not parsed and url content is returned in single cue`() = runTest {
        whenever(transcriptsManager.observerTranscriptForEpisode(any())).thenReturn(flowOf(transcript.copy(type = "text/html")))
        whenever(subtitleParserFactory.supportsFormat(any())).thenReturn(true)
        val htmlText = "<html>content</html>"
        initViewModel(htmlText)

        viewModel.parseAndLoadTranscript(isTranscriptViewOpen = true)

        viewModel.uiState.test {
            verifyNoInteractions(subtitleParserFactory)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given force refresh, when transcript load invoked, then transcript is refreshed`() = runTest {
        whenever(transcriptsManager.observerTranscriptForEpisode(any())).thenReturn(flowOf(transcript))
        whenever(subtitleParserFactory.supportsFormat(any())).thenReturn(true)
        initViewModel()

        viewModel.parseAndLoadTranscript(isTranscriptViewOpen = true, pulledToRefresh = true)

        verify(transcriptsManager).loadTranscript(transcript.url, forceRefresh = true)
    }

    @Test
    fun `given no force refresh, when transcript load invoked, then transcript is not refreshed`() = runTest {
        whenever(transcriptsManager.observerTranscriptForEpisode(any())).thenReturn(flowOf(transcript))
        whenever(subtitleParserFactory.supportsFormat(any())).thenReturn(true)
        initViewModel()

        viewModel.parseAndLoadTranscript(isTranscriptViewOpen = true, pulledToRefresh = false)

        verify(transcriptsManager).loadTranscript(transcript.url, forceRefresh = false)
    }

    @Test
    fun `given json format transcript, when transcript parse and load invoked, then transcript is parsed correctly`() = runTest {
        val jsonString = """
            {"version":"1.0.0","segments":[{"speaker":"Speaker 1","startTime":0,"endTime":10,"body":"Hello."},{"speaker":null,"startTime":11,"endTime":20,"body":"World!"}]}
        """.trimIndent()
        whenever(transcriptsManager.observerTranscriptForEpisode(any())).thenReturn(flowOf(transcript.copy(type = "application/json")))
        whenever(transcriptJsonConverter.fromString(jsonString)).thenReturn(
            listOf(
                TranscriptCue(speaker = "Speaker 1", startTime = 0.0, endTime = 10.0, body = "Hello."),
                TranscriptCue(speaker = null, startTime = 11.0, endTime = 20.0, body = "World!"),
            ),
        )
        initViewModel(jsonString)

        viewModel.parseAndLoadTranscript(
            isTranscriptViewOpen = true,
            pulledToRefresh = false,
        )

        viewModel.uiState.test {
            val state = awaitItem() as UiState.TranscriptLoaded
            assertTrue(
                state.displayInfo.items == listOf(
                    TranscriptViewModel.DisplayItem("Speaker 1", true, 2, 11),
                    TranscriptViewModel.DisplayItem("Hello.", false, 13, 19),
                    TranscriptViewModel.DisplayItem("World!", false, 21, 27),
                ),
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun initViewModel(
        content: String? = null,
        transcriptLoadException: Exception? = null,
    ) = runTest {
        whenever(playbackManager.playbackStateFlow).thenReturn(playbackStateFlow)
        if (transcriptLoadException != null) {
            given(transcriptsManager.loadTranscript(anyOrNull(), anyOrNull(), anyOrNull())).willAnswer { throw transcriptLoadException }
        } else {
            val response = mock<ResponseBody>()
            if (content != null) {
                whenever(response.string()).thenReturn(content)
            } else {
                whenever(response.bytes()).thenReturn(byteArrayOf())
            }
            whenever(transcriptsManager.loadTranscript(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(response)
        }

        viewModel = TranscriptViewModel(
            transcriptsManager = transcriptsManager,
            playbackManager = playbackManager,
            subtitleParserFactory = subtitleParserFactory,
            ioDispatcher = UnconfinedTestDispatcher(),
            analyticsTracker = mock(),
            transcriptJsonConverter = transcriptJsonConverter,
        )
    }
}
