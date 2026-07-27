package au.com.shiftyjelly.pocketcasts.playlists

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistIcon
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.repositories.playlist.ManualPlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylistPreview
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class TvPlaylistsViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val playlistPreviews = MutableSharedFlow<List<PlaylistPreview>>(replay = 1)
    private val playlistManager = mock<PlaylistManager> {
        on { playlistPreviewsFlow() } doReturn playlistPreviews
    }
    private val podcastDao = mock<PodcastDao>()

    @Test
    fun `state starts as loading`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvPlaylistsUiState.Loading, awaitItem())
        }
    }

    @Test
    fun `playlists load in manager order`() = runTest {
        val playlists = listOf(
            smartPreview(uuid = "playlist-1"),
            manualPreview(uuid = "playlist-2"),
            smartPreview(uuid = "playlist-3"),
        )
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvPlaylistsUiState.Loading, awaitItem())

            playlistPreviews.emit(playlists)

            assertEquals(TvPlaylistsUiState.Loaded(playlists), awaitItem())
        }
    }

    @Test
    fun `download playlists are sorted last`() = runTest {
        val downloadPlaylist = smartPreview(
            uuid = "playlist-1",
            downloadStatus = SmartRules.DownloadStatusRule.Downloaded,
        )
        val smartPlaylist = smartPreview(uuid = "playlist-2")
        val manualPlaylist = manualPreview(uuid = "playlist-3")
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvPlaylistsUiState.Loading, awaitItem())

            playlistPreviews.emit(listOf(downloadPlaylist, smartPlaylist, manualPlaylist))

            assertEquals(
                TvPlaylistsUiState.Loaded(listOf(smartPlaylist, manualPlaylist, downloadPlaylist)),
                awaitItem(),
            )
        }
    }

    @Test
    fun `empty playlists load as an empty list`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvPlaylistsUiState.Loading, awaitItem())

            playlistPreviews.emit(listOf(smartPreview(uuid = "playlist-1")))
            assertEquals(TvPlaylistsUiState.Loaded(listOf(smartPreview(uuid = "playlist-1"))), awaitItem())

            playlistPreviews.emit(emptyList())
            assertEquals(TvPlaylistsUiState.Loaded(emptyList()), awaitItem())
        }
    }

    @Test
    fun `podcast tint is looked up from the database`() = runTest {
        val tintedPodcast = Podcast(uuid = "podcast-1").apply { tintColorForLightBg = 0xFF123456.toInt() }
        val untintedPodcast = Podcast(uuid = "podcast-2")
        val podcastDao = mock<PodcastDao> {
            on { findPodcastByUuid("podcast-1") } doReturn tintedPodcast
            on { findPodcastByUuid("podcast-2") } doReturn untintedPodcast
        }
        val viewModel = createViewModel(podcastDao = podcastDao)

        assertEquals(0xFF123456.toInt(), viewModel.findPodcastTint("podcast-1"))
        assertNull(viewModel.findPodcastTint("podcast-2"))
        assertNull(viewModel.findPodcastTint("podcast-3"))
    }

    private fun createViewModel(podcastDao: PodcastDao = this.podcastDao) = TvPlaylistsViewModel(
        playlistManager = playlistManager,
        podcastDao = podcastDao,
    )

    private fun smartPreview(
        uuid: String,
        downloadStatus: SmartRules.DownloadStatusRule = SmartRules.DownloadStatusRule.Any,
    ) = SmartPlaylistPreview(
        uuid = uuid,
        title = "Playlist $uuid",
        settings = Playlist.Settings.ForPreview,
        icon = PlaylistIcon(0),
        smartRules = SmartRules.Default.copy(downloadStatus = downloadStatus),
    )

    private fun manualPreview(uuid: String) = ManualPlaylistPreview(
        uuid = uuid,
        title = "Playlist $uuid",
        settings = Playlist.Settings.ForPreview,
        icon = PlaylistIcon(0),
    )
}
