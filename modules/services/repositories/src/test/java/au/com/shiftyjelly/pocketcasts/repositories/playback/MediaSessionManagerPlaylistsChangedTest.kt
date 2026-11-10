package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.media3.session.MediaLibraryService
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistIcon
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.ManualPlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import com.automattic.eventhorizon.EventHorizon
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MediaSessionManagerPlaylistsChangedTest {
    private val playlistManager = mock<PlaylistManager>()
    private val session = mock<MediaLibraryService.MediaLibrarySession>()

    @Test
    fun `notifies the playlists root and every playlist`() = runTest {
        whenever(playlistManager.playlistPreviewsFlow()).thenReturn(
            flowOf(listOf(playlistPreview("playlist-1"), playlistPreview("playlist-2"))),
        )

        createManager().notifyPlaylistsChanged(session)

        verify(session).notifyChildrenChanged(eq(FILTERS_ROOT), eq(Int.MAX_VALUE), anyOrNull())
        verify(session).notifyChildrenChanged(eq("playlist-1"), eq(Int.MAX_VALUE), anyOrNull())
        verify(session).notifyChildrenChanged(eq("playlist-2"), eq(Int.MAX_VALUE), anyOrNull())
        verifyNoMoreInteractions(session)
    }

    @Test
    fun `notifies only the playlists root when there are no playlists`() = runTest {
        whenever(playlistManager.playlistPreviewsFlow()).thenReturn(flowOf(emptyList()))

        createManager().notifyPlaylistsChanged(session)

        verify(session).notifyChildrenChanged(eq(FILTERS_ROOT), eq(Int.MAX_VALUE), anyOrNull())
        verifyNoMoreInteractions(session)
    }

    private fun playlistPreview(uuid: String) = ManualPlaylistPreview(
        uuid = uuid,
        title = uuid,
        settings = Playlist.Settings.ForPreview,
        icon = PlaylistIcon(0),
    )

    private fun TestScope.createManager() = MediaSessionManager(
        playbackManager = mock<PlaybackManager>(),
        podcastManager = mock<PodcastManager>(),
        episodeManager = mock<EpisodeManager>(),
        playlistManager = playlistManager,
        settings = mock<Settings>(),
        context = RuntimeEnvironment.getApplication(),
        eventHorizon = mock<EventHorizon>(),
        errorReporter = mock<PlaybackServiceErrorReporter>(),
        bookmarkManager = mock<BookmarkManager>(),
        browseTreeProvider = mock<BrowseTreeProvider>(),
        applicationScope = this,
    )
}
