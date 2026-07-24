package au.com.shiftyjelly.pocketcasts.playlists.manual

import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeUuidPair
import au.com.shiftyjelly.pocketcasts.playlists.create.FakePlaylistManager
import au.com.shiftyjelly.pocketcasts.playlists.manual.AddToPlaylistViewModel.PlaylistChangeFeedback
import au.com.shiftyjelly.pocketcasts.playlists.manual.AddToPlaylistViewModel.PlaylistChangeFeedback.PluralResource
import au.com.shiftyjelly.pocketcasts.playlists.manual.AddToPlaylistViewModel.PlaylistChangeFeedback.StringResource
import au.com.shiftyjelly.pocketcasts.playlists.manual.AddToPlaylistViewModel.PlaylistChangeSummary
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.views.swipe.AddToPlaylistFragmentFactory
import com.automattic.eventhorizon.EventHorizon
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class AddToPlaylistViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val playlistManager = FakePlaylistManager()

    private lateinit var viewModel: AddToPlaylistViewModel

    @Before
    fun setUp() {
        viewModel = AddToPlaylistViewModel(
            playlistManager = playlistManager,
            eventHorizon = EventHorizon(TestEventSink()),
            source = AddToPlaylistFragmentFactory.Source.Shelf,
            episodeUuids = List(3) { index ->
                EpisodeUuidPair("episode-uuid-$index", "podcast-uuid-$index")
            },
            initialPlaylistTitle = "Title",
        )
    }

    @Test
    fun `playlist change summary reports net added and removed playlist counts`() = runTest(coroutineRule.testDispatcher) {
        assertEquals(
            PlaylistChangeSummary(addedCount = 0, removedCount = 0),
            viewModel.getPlaylistChangeSummary(),
        )

        viewModel.addToPlaylist("playlist-uuid-1")
        viewModel.addToPlaylist("playlist-uuid-2")
        viewModel.removeFromPlaylist("playlist-uuid-3")

        assertEquals(
            PlaylistChangeSummary(addedCount = 2, removedCount = 1),
            viewModel.getPlaylistChangeSummary(),
        )

        viewModel.removeFromPlaylist("playlist-uuid-2")
        viewModel.addToPlaylist("playlist-uuid-3")

        assertEquals(
            PlaylistChangeSummary(addedCount = 1, removedCount = 0),
            viewModel.getPlaylistChangeSummary(),
        )
    }

    @Test
    fun `playlist change feedback selects the correct message for every outcome`() = runTest(coroutineRule.testDispatcher) {
        val cases: List<Pair<PlaylistChangeSummary, PlaylistChangeFeedback>> = listOf(
            PlaylistChangeSummary(addedCount = 0, removedCount = 0) to PlaylistChangeFeedback.None,
            PlaylistChangeSummary(addedCount = 1, removedCount = 0) to
                StringResource(LR.string.added_to_playlist_feedback),
            PlaylistChangeSummary(addedCount = 2, removedCount = 0) to
                PluralResource(LR.plurals.added_to_playlist_single_multiple, quantity = 2),
            PlaylistChangeSummary(addedCount = 0, removedCount = 1) to
                StringResource(LR.string.removed_from_playlist_feedback),
            PlaylistChangeSummary(addedCount = 0, removedCount = 2) to
                PluralResource(LR.plurals.removed_from_playlists, quantity = 2),
            PlaylistChangeSummary(addedCount = 1, removedCount = 1) to
                PluralResource(LR.plurals.changed_playlists, quantity = 2),
            PlaylistChangeSummary(addedCount = 2, removedCount = 1) to
                PluralResource(LR.plurals.changed_playlists, quantity = 3),
        )

        cases.forEach { (summary, expectedFeedback) ->
            assertEquals(expectedFeedback, PlaylistChangeFeedback.from(summary))
        }
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
        repeat(3) { index ->
            assertEquals(
                "playlist-uuid-1" to "episode-uuid-$index",
                playlistManager.addManualEpisodeTurbine.awaitItem(),
            )
        }
        repeat(3) { index ->
            assertEquals(
                "playlist-uuid-3" to "episode-uuid-$index",
                playlistManager.addManualEpisodeTurbine.awaitItem(),
            )
        }
        repeat(3) { index ->
            assertEquals(
                "playlist-uuid-4" to "episode-uuid-$index",
                playlistManager.deleteManualEpisodeTurbine.awaitItem(),
            )
        }
        playlistManager.addManualEpisodeTurbine.expectNoEvents()
        playlistManager.deleteManualEpisodeTurbine.expectNoEvents()
    }
}
