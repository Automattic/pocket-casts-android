package au.com.shiftyjelly.pocketcasts.repositories.playlist

import android.content.Context
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playlist.DefaultPlaylistsInitializerImpl.Companion.CREATED_DEFAULT_PLAYLISTS_KEY
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class DefaultPlaylistsInitializerImplTest {
    private val settings = mock<Settings>()
    private val playlistManager = mock<PlaylistManager>()
    private val context = mock<Context> {
        on { getString(LR.string.filters_title_in_progress) } doReturn "In Progress"
        on { getString(LR.string.filters_title_new_releases) } doReturn "New Releases"
    }
    private val initializer = DefaultPlaylistsInitializerImpl(settings, playlistManager, context)

    @Test
    fun `seed the default playlists on first initialization`() = runTest {
        whenever(settings.getBooleanForKey(CREATED_DEFAULT_PLAYLISTS_KEY, false)).thenReturn(false)
        whenever(playlistManager.createSmartPlaylist(SmartPlaylistDraft.InProgress)).thenReturn("in-progress-uuid")
        whenever(playlistManager.createSmartPlaylist(SmartPlaylistDraft.NewReleases)).thenReturn("new-releases-uuid")

        initializer.initialize()

        inOrder(playlistManager) {
            verify(playlistManager).createSmartPlaylist(SmartPlaylistDraft.InProgress)
            verify(playlistManager).updateName("in-progress-uuid", "In Progress")
            verify(playlistManager).createSmartPlaylist(SmartPlaylistDraft.NewReleases)
            verify(playlistManager).updateName("new-releases-uuid", "New Releases")
        }
        verify(settings).setBooleanForKey(CREATED_DEFAULT_PLAYLISTS_KEY, true)
    }

    @Test
    fun `do not seed again once the playlists were created`() = runTest {
        whenever(settings.getBooleanForKey(CREATED_DEFAULT_PLAYLISTS_KEY, false)).thenReturn(true)

        initializer.initialize()

        verify(playlistManager, never()).createSmartPlaylist(any())
    }

    @Test
    fun `seed again when forced`() = runTest {
        whenever(settings.getBooleanForKey(CREATED_DEFAULT_PLAYLISTS_KEY, false)).thenReturn(true)
        whenever(playlistManager.createSmartPlaylist(SmartPlaylistDraft.InProgress)).thenReturn("in-progress-uuid")
        whenever(playlistManager.createSmartPlaylist(SmartPlaylistDraft.NewReleases)).thenReturn("new-releases-uuid")

        initializer.initialize(force = true)

        verify(playlistManager).createSmartPlaylist(SmartPlaylistDraft.InProgress)
        verify(playlistManager).createSmartPlaylist(SmartPlaylistDraft.NewReleases)
    }
}
