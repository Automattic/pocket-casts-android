package au.com.shiftyjelly.pocketcasts.playlists.manual

import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeUuidPair
import au.com.shiftyjelly.pocketcasts.playlists.create.FakePlaylistManager
import au.com.shiftyjelly.pocketcasts.playlists.manual.AddToPlaylistViewModel.AddedPlaylist
import au.com.shiftyjelly.pocketcasts.playlists.manual.AddToPlaylistViewModel.PlaylistChangeFeedback
import au.com.shiftyjelly.pocketcasts.playlists.manual.AddToPlaylistViewModel.PlaylistChangeFeedback.PluralResource
import au.com.shiftyjelly.pocketcasts.playlists.manual.AddToPlaylistViewModel.PlaylistChangeFeedback.SinglePlaylistAddition
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

        viewModel.addToPlaylist("playlist-uuid-1", "Playlist 1")
        viewModel.addToPlaylist("playlist-uuid-2", "Playlist 2")
        viewModel.removeFromPlaylist("playlist-uuid-3")

        assertEquals(
            PlaylistChangeSummary(addedCount = 2, removedCount = 1),
            viewModel.getPlaylistChangeSummary(),
        )

        viewModel.removeFromPlaylist("playlist-uuid-2")
        viewModel.addToPlaylist("playlist-uuid-3", "Playlist 3")

        assertEquals(
            PlaylistChangeSummary(addedCount = 1, removedCount = 0),
            viewModel.getPlaylistChangeSummary(),
        )
    }

    @Test
    fun `playlist change feedback selects the correct message for every outcome`() = runTest(coroutineRule.testDispatcher) {
        val addedPlaylist = AddedPlaylist(uuid = "playlist-uuid", title = "Playlist title")
        val cases = listOf(
            FeedbackCase(
                summary = PlaylistChangeSummary(addedCount = 0, removedCount = 0),
                singleAddedPlaylist = null,
                expectedFeedback = PlaylistChangeFeedback.None,
            ),
            FeedbackCase(
                summary = PlaylistChangeSummary(addedCount = 1, removedCount = 0),
                singleAddedPlaylist = addedPlaylist,
                expectedFeedback = SinglePlaylistAddition(
                    resourceId = LR.string.added_to_playlist_single,
                    playlist = addedPlaylist,
                ),
            ),
            FeedbackCase(
                summary = PlaylistChangeSummary(addedCount = 2, removedCount = 0),
                singleAddedPlaylist = null,
                expectedFeedback = PluralResource(LR.plurals.added_to_playlist_single_multiple, quantity = 2),
            ),
            FeedbackCase(
                summary = PlaylistChangeSummary(addedCount = 0, removedCount = 1),
                singleAddedPlaylist = null,
                expectedFeedback = StringResource(LR.string.removed_from_playlist_feedback),
            ),
            FeedbackCase(
                summary = PlaylistChangeSummary(addedCount = 0, removedCount = 2),
                singleAddedPlaylist = null,
                expectedFeedback = PluralResource(LR.plurals.removed_from_playlists, quantity = 2),
            ),
            FeedbackCase(
                summary = PlaylistChangeSummary(addedCount = 1, removedCount = 1),
                singleAddedPlaylist = addedPlaylist,
                expectedFeedback = PluralResource(LR.plurals.changed_playlists, quantity = 2),
            ),
            FeedbackCase(
                summary = PlaylistChangeSummary(addedCount = 2, removedCount = 1),
                singleAddedPlaylist = null,
                expectedFeedback = PluralResource(LR.plurals.changed_playlists, quantity = 3),
            ),
        )

        cases.forEach { case ->
            assertEquals(
                case.expectedFeedback,
                PlaylistChangeFeedback.from(case.summary, case.singleAddedPlaylist),
            )
        }
    }

    @Test
    fun `single playlist addition feedback keeps the cached playlist title and uuid`() = runTest(
        coroutineRule.testDispatcher,
    ) {
        viewModel.addToPlaylist(
            playlistUuid = "playlist-uuid",
            playlistTitle = "Playlist title",
        )
        viewModel.addToPlaylist(
            playlistUuid = "other-playlist-uuid",
            playlistTitle = "Other playlist",
        )
        viewModel.removeFromPlaylist("other-playlist-uuid")

        assertEquals(
            SinglePlaylistAddition(
                resourceId = LR.string.added_to_playlist_single,
                playlist = AddedPlaylist(
                    uuid = "playlist-uuid",
                    title = "Playlist title",
                ),
            ),
            viewModel.getPlaylistChangeFeedback(),
        )
    }

    @Test
    fun `submit playlist changes only when committing`() = runTest(coroutineRule.testDispatcher) {
        viewModel.addToPlaylist("playlist-uuid-1", "Playlist 1")
        viewModel.removeFromPlaylist("playlist-uuid-1")
        viewModel.addToPlaylist("playlist-uuid-1", "Playlist 1")

        viewModel.addToPlaylist("playlist-uuid-2", "Playlist 2")
        viewModel.removeFromPlaylist("playlist-uuid-2")

        viewModel.addToPlaylist("playlist-uuid-3", "Playlist 3")

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

    private data class FeedbackCase(
        val summary: PlaylistChangeSummary,
        val singleAddedPlaylist: AddedPlaylist?,
        val expectedFeedback: PlaylistChangeFeedback,
    )
}
