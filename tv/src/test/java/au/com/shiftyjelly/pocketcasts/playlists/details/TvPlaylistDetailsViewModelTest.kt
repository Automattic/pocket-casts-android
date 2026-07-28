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
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class TvPlaylistDetailsViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val playlists = MutableSharedFlow<ManualPlaylist?>(replay = 1)
    private val playlistManager = mock<PlaylistManager> {
        on { manualPlaylistFlow(any(), anyOrNull(), any()) } doReturn playlists
    }
    private val preferences = mock<TvPlaylistPreferences>()

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
            assertEquals(false, state.isShowingArchived)
        }
    }

    @Test
    fun `archived episodes are shown when the stored preference allows them`() = runTest {
        val preferences = mock<TvPlaylistPreferences> {
            on { isShowingArchived("playlist-uuid") } doReturn true
        }
        val viewModel = createViewModel(preferences)

        viewModel.uiState.test {
            assertEquals(TvPlaylistDetailsUiState.Loading, awaitItem())

            playlists.emit(playlist(availableEpisode, archivedEpisode))

            val state = awaitItem() as TvPlaylistDetailsUiState.Loaded
            assertEquals(listOf(availableEpisode, archivedEpisode), state.episodes)
            assertEquals(true, state.isShowingArchived)
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
            assertEquals(true, state.isShowingArchived)
            verify(preferences).setShowingArchived(eq("playlist-uuid"), eq(true))
        }
    }

    private fun createViewModel(prefs: TvPlaylistPreferences = preferences) = TvPlaylistDetailsViewModel(
        playlistUuid = "playlist-uuid",
        playlistType = Playlist.Type.Manual,
        playlistManager = playlistManager,
        preferences = prefs,
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
