package au.com.shiftyjelly.pocketcasts.playlists.details

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.preferences.TvPreferences
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlayAllHandler
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlayAllResponse
import au.com.shiftyjelly.pocketcasts.repositories.playlist.ManualPlaylist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import java.util.Date
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TvPlaylistDetailsViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val playlists = MutableSharedFlow<ManualPlaylist?>(replay = 1)
    private val playlistManager = mock<PlaylistManager> {
        on { manualPlaylistFlow(any(), anyOrNull(), any()) } doReturn playlists
    }
    private val preferences = mock<TvPreferences>()

    private val playAllHandler = mock<PlayAllHandler>()
    private val playAllHandlerFactory = mock<PlayAllHandler.Factory> {
        on { create(any()) } doReturn playAllHandler
    }

    private val availableEpisode = episode(uuid = "episode-1", isArchived = false)
    private val archivedEpisode = episode(uuid = "episode-2", isArchived = true)

    @Test
    fun `archived episodes are hidden by default`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvPlaylistDetailsUiState.Loading, awaitItem())

            playlists.emit(playlist(availableEpisode, archivedEpisode))

            val state = awaitItem() as TvPlaylistDetailsUiState.Loaded
            assertEquals(listOf(availableEpisode), state.episodes)
            assertEquals(2, state.availableEpisodeCount)
            assertEquals(false, state.isShowingArchivedOnDevice)
        }
    }

    @Test
    fun `archived episodes are shown when the stored preference allows them`() = runTest {
        val preferences = mock<TvPreferences> {
            on { isPlaylistShowingArchived("playlist-uuid") } doReturn true
        }
        val viewModel = createViewModel(preferences)

        viewModel.uiState.test {
            assertEquals(TvPlaylistDetailsUiState.Loading, awaitItem())

            playlists.emit(playlist(availableEpisode, archivedEpisode))

            val state = awaitItem() as TvPlaylistDetailsUiState.Loaded
            assertEquals(listOf(availableEpisode, archivedEpisode), state.episodes)
            assertEquals(true, state.isShowingArchivedOnDevice)
        }
    }

    @Test
    fun `only available episodes are surfaced`() = runTest {
        val available = episode(uuid = "available", isArchived = false)
        val unavailable = PlaylistEpisode.Unavailable(ManualPlaylistEpisode.test(episodeUuid = "unavailable"))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvPlaylistDetailsUiState.Loading, awaitItem())

            playlists.emit(
                ManualPlaylist(
                    uuid = "playlist-uuid",
                    title = "Playlist",
                    episodes = listOf(PlaylistEpisode.Available(available), unavailable),
                    settings = Playlist.Settings.ForPreview,
                    metadata = Playlist.Metadata.ForPreview,
                ),
            )

            val state = awaitItem() as TvPlaylistDetailsUiState.Loaded
            assertEquals(listOf(available), state.episodes)
        }
    }

    @Test
    fun `deleted playlist resolves to the not found state`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvPlaylistDetailsUiState.Loading, awaitItem())

            playlists.emit(playlist(availableEpisode))
            assertEquals(listOf(availableEpisode), (awaitItem() as TvPlaylistDetailsUiState.Loaded).episodes)

            playlists.emit(null)
            assertEquals(TvPlaylistDetailsUiState.NotFound, awaitItem())
        }
    }

    @Test
    fun `toggling the archive filter updates the list and persists the preference`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvPlaylistDetailsUiState.Loading, awaitItem())

            playlists.emit(playlist(availableEpisode, archivedEpisode))
            assertEquals(listOf(availableEpisode), (awaitItem() as TvPlaylistDetailsUiState.Loaded).episodes)

            viewModel.toggleArchiveFilter()

            val state = awaitItem() as TvPlaylistDetailsUiState.Loaded
            assertEquals(listOf(availableEpisode, archivedEpisode), state.episodes)
            assertEquals(true, state.isShowingArchivedOnDevice)
            verify(preferences).setPlaylistShowingArchived(eq("playlist-uuid"), eq(true))
        }
    }

    @Test
    fun `play all opens now playing when the queue does not need replacing`() = runTest {
        whenever(playAllHandler.handlePlayAllEpisodes(any())) doReturn PlayAllResponse.DoNothing
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvPlaylistDetailsUiState.Loading, awaitItem())
            playlists.emit(playlist(availableEpisode))
            assertEquals(listOf(availableEpisode), (awaitItem() as TvPlaylistDetailsUiState.Loaded).episodes)

            viewModel.events.test {
                viewModel.playAll()
                assertEquals(TvPlaylistDetailsEvent.OpenNowPlaying, awaitItem())
            }
        }
    }

    @Test
    fun `play all asks for confirmation when the queue would be replaced`() = runTest {
        whenever(playAllHandler.handlePlayAllEpisodes(any())) doReturn PlayAllResponse.ShowWarning
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvPlaylistDetailsUiState.Loading, awaitItem())
            playlists.emit(playlist(availableEpisode))
            assertEquals(listOf(availableEpisode), (awaitItem() as TvPlaylistDetailsUiState.Loaded).episodes)

            viewModel.events.test {
                viewModel.playAll()
                assertEquals(TvPlaylistDetailsEvent.ShowReplaceUpNextConfirmation, awaitItem())
            }
        }
    }

    @Test
    fun `replacing without saving plays the pending episodes and opens now playing`() = runTest {
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.replaceUpNextAndPlay(saveUpNext = false, upNextName = "Up Next")
            assertEquals(TvPlaylistDetailsEvent.OpenNowPlaying, awaitItem())
        }

        verify(playAllHandler, never()).saveUpNextAsPlaylist(any())
        verify(playAllHandler).playAllPendingEpisodes()
    }

    @Test
    fun `replacing with saving saves the queue before playing`() = runTest {
        whenever(playAllHandler.saveUpNextAsPlaylist(any())) doReturn true
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.replaceUpNextAndPlay(saveUpNext = true, upNextName = "Up Next")
            assertEquals(TvPlaylistDetailsEvent.ShowUpNextSavedToast, awaitItem())
            assertEquals(TvPlaylistDetailsEvent.OpenNowPlaying, awaitItem())
        }

        inOrder(playAllHandler) {
            verify(playAllHandler).saveUpNextAsPlaylist("Up Next")
            verify(playAllHandler).playAllPendingEpisodes()
        }
    }

    @Test
    fun `replacing with saving does not claim a save when nothing was saved`() = runTest {
        whenever(playAllHandler.saveUpNextAsPlaylist(any())) doReturn false
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.replaceUpNextAndPlay(saveUpNext = true, upNextName = "Up Next")
            assertEquals(TvPlaylistDetailsEvent.OpenNowPlaying, awaitItem())
        }

        verify(playAllHandler).playAllPendingEpisodes()
    }

    @Test
    fun `replacing with saving still plays when the save fails`() = runTest {
        whenever(playAllHandler.saveUpNextAsPlaylist(any())) doSuspendableAnswer { throw RuntimeException("boom") }
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.replaceUpNextAndPlay(saveUpNext = true, upNextName = "Up Next")
            assertEquals(TvPlaylistDetailsEvent.OpenNowPlaying, awaitItem())
        }

        verify(playAllHandler).playAllPendingEpisodes()
    }

    @Test
    fun `rapid replace requests save the up next only once`() = runTest {
        val gate = CompletableDeferred<Boolean>()
        whenever(playAllHandler.saveUpNextAsPlaylist(any())) doSuspendableAnswer { gate.await() }
        val viewModel = createViewModel()

        viewModel.replaceUpNextAndPlay(saveUpNext = true, upNextName = "Up Next")
        viewModel.replaceUpNextAndPlay(saveUpNext = true, upNextName = "Up Next")
        gate.complete(true)

        verify(playAllHandler, times(1)).saveUpNextAsPlaylist("Up Next")
    }

    @Test
    fun `play all passes only the visible episodes to the handler`() = runTest {
        whenever(playAllHandler.handlePlayAllEpisodes(any())) doReturn PlayAllResponse.DoNothing
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvPlaylistDetailsUiState.Loading, awaitItem())
            playlists.emit(playlist(availableEpisode, archivedEpisode))
            assertEquals(listOf(availableEpisode), (awaitItem() as TvPlaylistDetailsUiState.Loaded).episodes)

            viewModel.playAll()
        }

        verify(playAllHandler).handlePlayAllEpisodes(listOf(availableEpisode))
    }

    @Test
    fun `play all surfaces the no episodes toast when nothing can be played`() = runTest {
        whenever(playAllHandler.handlePlayAllEpisodes(any())) doReturn PlayAllResponse.ShowNoEpisodesToPlay
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvPlaylistDetailsUiState.Loading, awaitItem())
            playlists.emit(playlist(availableEpisode))
            assertEquals(listOf(availableEpisode), (awaitItem() as TvPlaylistDetailsUiState.Loaded).episodes)

            viewModel.events.test {
                viewModel.playAll()
                assertEquals(TvPlaylistDetailsEvent.ShowNoEpisodesToPlay, awaitItem())
            }
        }
    }

    private fun createViewModel(prefs: TvPreferences = preferences) = TvPlaylistDetailsViewModel(
        playlistUuid = "playlist-uuid",
        playlistType = Playlist.Type.Manual,
        playlistManager = playlistManager,
        preferences = prefs,
        playAllHandlerFactory = playAllHandlerFactory,
    )

    private fun playlist(vararg episodes: PodcastEpisode) = ManualPlaylist(
        uuid = "playlist-uuid",
        title = "Playlist",
        episodes = episodes.map(PlaylistEpisode::Available),
        settings = Playlist.Settings.ForPreview,
        metadata = Playlist.Metadata.ForPreview,
    )

    private fun episode(uuid: String, isArchived: Boolean) = PodcastEpisode(
        uuid = uuid,
        publishedDate = Date(0),
        isArchived = isArchived,
    )
}
