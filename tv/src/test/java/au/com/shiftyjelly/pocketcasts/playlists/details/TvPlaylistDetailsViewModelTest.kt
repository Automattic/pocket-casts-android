package au.com.shiftyjelly.pocketcasts.playlists.details

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playlist.ManualPlaylist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class TvPlaylistDetailsViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val playlists = MutableSharedFlow<ManualPlaylist?>(replay = 1)
    private val playlistManager = mock<PlaylistManager> {
        on { manualPlaylistFlow(any(), anyOrNull()) } doReturn playlists
    }

    private val episode = episode(uuid = "episode-1")

    @Test
    fun `playlist episodes load`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvPlaylistDetailsUiState.Loading, awaitItem())

            playlists.emit(playlist(episode))

            val state = awaitItem() as TvPlaylistDetailsUiState.Loaded
            assertEquals(listOf(episode), state.episodes)
        }
    }

    @Test
    fun `only available episodes are surfaced`() = runTest {
        val available = episode(uuid = "available")
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

            playlists.emit(playlist(episode))
            assertEquals(listOf(episode), (awaitItem() as TvPlaylistDetailsUiState.Loaded).episodes)

            playlists.emit(null)
            assertEquals(TvPlaylistDetailsUiState.NotFound, awaitItem())
        }
    }

    private fun createViewModel() = TvPlaylistDetailsViewModel(
        playlistUuid = "playlist-uuid",
        playlistType = Playlist.Type.Manual,
        playlistManager = playlistManager,
    )

    private fun playlist(vararg episodes: PodcastEpisode) = ManualPlaylist(
        uuid = "playlist-uuid",
        title = "Playlist",
        episodes = episodes.map(PlaylistEpisode::Available),
        settings = Playlist.Settings.ForPreview,
        metadata = Playlist.Metadata.ForPreview,
    )

    private fun episode(uuid: String) = PodcastEpisode(
        uuid = uuid,
        publishedDate = Date(0),
    )
}
