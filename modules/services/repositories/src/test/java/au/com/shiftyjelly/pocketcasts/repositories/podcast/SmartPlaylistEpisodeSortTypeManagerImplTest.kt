package au.com.shiftyjelly.pocketcasts.repositories.podcast

import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.SmartPlaylistDao
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationManager
import au.com.shiftyjelly.pocketcasts.repositories.notification.OnboardingNotificationType
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SmartPlaylistEpisodeSortTypeManagerImplTest {

    private val settings: Settings = mock()
    private val downloadManager: DownloadManager = mock()
    private val playlistUpdateAnalytics: PlaylistUpdateAnalytics = mock()
    private val syncManager: SyncManager = mock()
    private val notificationManager: NotificationManager = mock()
    private val context: Context = mock()
    private val appDatabase: AppDatabase = mock()
    private val smartPlaylistDao: SmartPlaylistDao = mock()

    @Test
    fun `should mark the user as having interacted with the feature when creating a filter`() = runTest {
        val playlistManager = initViewModel()

        playlistManager.updateBlocking(
            smartPlaylist = SmartPlaylist(),
            userPlaylistUpdate = UserPlaylistUpdate(listOf(PlaylistProperty.Color), PlaylistUpdateSource.AUTO_DOWNLOAD_SETTINGS),
            isCreatingFilter = true,
        )

        advanceUntilIdle()

        verify(notificationManager).updateUserFeatureInteraction(OnboardingNotificationType.Filters)
    }

    @Test
    fun `should not mark the user as having interacted with the feature when a filter is not being created`() = runTest {
        val playlistManager = initViewModel()

        playlistManager.updateBlocking(
            smartPlaylist = SmartPlaylist(),
            userPlaylistUpdate = UserPlaylistUpdate(listOf(PlaylistProperty.Color), PlaylistUpdateSource.AUTO_DOWNLOAD_SETTINGS),
            isCreatingFilter = false,
        )

        advanceUntilIdle()

        verify(notificationManager, never()).updateUserFeatureInteraction(OnboardingNotificationType.Filters)
    }

    private fun initViewModel(): SmartPlaylistManagerImpl {
        whenever(appDatabase.smartPlaylistDao()).thenReturn(smartPlaylistDao)
        whenever(smartPlaylistDao.updateBlocking(any())).then { }
        whenever(playlistUpdateAnalytics.update(any(), any(), any())).then { }

        return SmartPlaylistManagerImpl(
            settings = settings,
            downloadManager = downloadManager,
            playlistUpdateAnalytics = playlistUpdateAnalytics,
            syncManager = syncManager,
            notificationManager = notificationManager,
            context = context,
            appDatabase = appDatabase,
        )
    }
}
