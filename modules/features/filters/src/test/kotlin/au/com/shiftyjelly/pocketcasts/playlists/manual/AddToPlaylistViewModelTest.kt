package au.com.shiftyjelly.pocketcasts.playlists.manual

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.playlists.create.FakePlaylistManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.views.swipe.AddToPlaylistFragmentFactory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AddToPlaylistViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val playlistManager = FakePlaylistManager()

    private lateinit var viewModel: AddToPlaylistViewModel

    @Before
    fun setUp() {
        viewModel = AddToPlaylistViewModel(
            playlistManager = playlistManager,
            tracker = AnalyticsTracker.test(),
            source = AddToPlaylistFragmentFactory.Source.Shelf,
            episodeUuid = "episode-uuid",
            podcastUuid = "podcast-uuid",
            initialPlaylistTitle = "Title",
        )
    }

    @Test
    fun `submit playlist changes only when committing`() = runTest(coroutineRule.testDispatcher) {
        viewModel.addToPlaylist("playlist-uuid-1")
        viewModel.removeFromPlaylist("playlist-uuid-1")
        viewModel.addToPlaylist("playlist-uuid-1")

        viewModel.addToPlaylist("playlist-uuid-2")
        viewModel.removeFromPlaylist("playlist-uuid-2")

        viewModel.addToPlaylist("playlist-uuid-3")
        viewModel.removeFromPlaylist("playlist-uuid-4")

        playlistManager.addManualEpisodeTurbine.expectNoEvents()
        playlistManager.deleteManualEpisodeTurbine.expectNoEvents()

        viewModel.commitPlaylistChanges()
        assertEquals(
            "playlist-uuid-1" to "episode-uuid",
            playlistManager.addManualEpisodeTurbine.awaitItem(),
        )
        assertEquals(
            "playlist-uuid-3" to "episode-uuid",
            playlistManager.addManualEpisodeTurbine.awaitItem(),
        )
        assertEquals(
            "playlist-uuid-4" to "episode-uuid",
            playlistManager.deleteManualEpisodeTurbine.awaitItem(),
        )
        playlistManager.addManualEpisodeTurbine.expectNoEvents()
        playlistManager.deleteManualEpisodeTurbine.expectNoEvents()
    }
}
