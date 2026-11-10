package au.com.shiftyjelly.pocketcasts.starred

import android.content.Context
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.StarredShownEvent
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking

@OptIn(ExperimentalCoroutinesApi::class)
class TvStarredViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val starredEpisodes = MutableSharedFlow<List<PodcastEpisode>>(replay = 1)
    private val episodeManager = mock<EpisodeManager> {
        on { findStarredEpisodesFlow(any()) } doReturn starredEpisodes
    }
    private val syncManager = mock<SyncManager>()
    private val context = mock<Context>()
    private val playbackManager = mock<PlaybackManager>()
    private val eventHorizon = mock<EventHorizon>()

    @Test
    fun `state starts as loading`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvStarredUiState.Loading, awaitItem())
        }
    }

    @Test
    fun `starred episodes load reactively`() = runTest {
        val viewModel = createViewModel()
        val episodes = listOf(episode("first"), episode("second"))

        viewModel.uiState.test {
            assertEquals(TvStarredUiState.Loading, awaitItem())

            starredEpisodes.emit(episodes)

            assertEquals(TvStarredUiState.Loaded(episodes), awaitItem())
        }
    }

    @Test
    fun `an empty starred list maps to the empty state`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvStarredUiState.Loading, awaitItem())

            starredEpisodes.emit(emptyList())

            assertEquals(TvStarredUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `a starred list that empties maps back to the empty state`() = runTest {
        val viewModel = createViewModel()
        val episode = episode("starred")

        viewModel.uiState.test {
            assertEquals(TvStarredUiState.Loading, awaitItem())

            starredEpisodes.emit(listOf(episode))
            assertEquals(TvStarredUiState.Loaded(listOf(episode)), awaitItem())

            starredEpisodes.emit(emptyList())
            assertEquals(TvStarredUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `play starts playback of the episode from the starred source`() = runTest {
        val episode = episode("episode")

        createViewModel().play(episode)

        verifyBlocking(playbackManager) { playNowSuspend(episode = episode, sourceView = SourceView.STARRED) }
    }

    @Test
    fun `showing the screen tracks the starred shown event`() = runTest {
        createViewModel().trackStarredShown()

        verify(eventHorizon).track(StarredShownEvent)
    }

    @Test
    fun `showing the screen checks whether the user is signed in before syncing`() = runTest {
        createViewModel().onShown()

        verify(syncManager).isLoggedIn()
    }

    private fun createViewModel() = TvStarredViewModel(
        context = context,
        episodeManager = episodeManager,
        syncManager = syncManager,
        playbackManager = playbackManager,
        eventHorizon = eventHorizon,
    )

    private fun episode(uuid: String) = PodcastEpisode(uuid = uuid, publishedDate = Date(0))
}
