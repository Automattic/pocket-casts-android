package au.com.shiftyjelly.pocketcasts.history

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.ListeningHistoryShownEvent
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking

@OptIn(ExperimentalCoroutinesApi::class)
class TvListeningHistoryViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val history = MutableSharedFlow<List<PodcastEpisode>>(replay = 1, extraBufferCapacity = 1)
    private val episodeManager = mock<EpisodeManager> {
        on { findPlaybackHistoryEpisodesFlow() } doReturn history
    }
    private val playbackManager = mock<PlaybackManager>()
    private val eventHorizon = mock<EventHorizon>()

    @Test
    fun `an empty history maps to the empty state`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvListeningHistoryUiState.Loading, awaitItem())

            history.emit(emptyList())

            assertEquals(TvListeningHistoryUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `history episodes are exposed in the loaded state`() = runTest {
        val viewModel = createViewModel()
        val first = episode("first")
        val second = episode("second")

        viewModel.uiState.test {
            assertEquals(TvListeningHistoryUiState.Loading, awaitItem())

            history.emit(listOf(first, second))

            assertEquals(TvListeningHistoryUiState.Loaded(listOf(first, second)), awaitItem())
        }
    }

    @Test
    fun `history that empties maps back to the empty state`() = runTest {
        val viewModel = createViewModel()
        val played = episode("played")

        viewModel.uiState.test {
            assertEquals(TvListeningHistoryUiState.Loading, awaitItem())

            history.emit(listOf(played))
            assertEquals(TvListeningHistoryUiState.Loaded(listOf(played)), awaitItem())

            history.emit(emptyList())
            assertEquals(TvListeningHistoryUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `play starts playback from the listening history source`() = runTest {
        val episode = episode("episode")

        createViewModel().play(episode)

        verifyBlocking(playbackManager) { playNowSuspend(episode = episode, sourceView = SourceView.LISTENING_HISTORY) }
    }

    @Test
    fun `showing the screen tracks the listening history shown event`() = runTest {
        createViewModel().trackShown()

        verify(eventHorizon).track(ListeningHistoryShownEvent)
    }

    private fun createViewModel() = TvListeningHistoryViewModel(
        episodeManager = episodeManager,
        playbackManager = playbackManager,
        eventHorizon = eventHorizon,
    )

    private fun episode(uuid: String) = PodcastEpisode(uuid = uuid, publishedDate = Date(0))
}
