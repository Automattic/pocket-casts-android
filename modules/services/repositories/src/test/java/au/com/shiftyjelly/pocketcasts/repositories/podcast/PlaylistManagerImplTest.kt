package au.com.shiftyjelly.pocketcasts.repositories.podcast

import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.PlaylistDao
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationManager
import au.com.shiftyjelly.pocketcasts.repositories.notification.OnboardingNotificationType
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistManagerImplTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val settings: Settings = mock()
    private val downloadManager: DownloadManager = mock()
    private val playlistUpdateAnalytics: PlaylistUpdateAnalytics = mock()
    private val syncManager: SyncManager = mock()
    private val notificationManager: NotificationManager = mock()
    private val context: Context = mock()
    private val appDatabase: AppDatabase = mock()
    private val playlistDao: PlaylistDao = mock()

    @Test
    fun `should mark the user as having interacted with the feature when creating a filter`() = runTest {
        val playlistManager = initViewModel()

        playlistManager.updateBlocking(mock(), mock(), isCreatingFilter = true)

        advanceUntilIdle()

        verify(notificationManager).updateUserFeatureInteraction(OnboardingNotificationType.Filters)
    }

    @Test
    fun `should not mark the user as having interacted with the feature when a filter is not being created`() = runTest {
        val playlistManager = initViewModel()

        playlistManager.updateBlocking(mock(), mock(), isCreatingFilter = false)

        advanceUntilIdle()

        verify(notificationManager, never()).updateUserFeatureInteraction(OnboardingNotificationType.Filters)
    }

    private fun initViewModel(): PlaylistManagerImpl {
        whenever(appDatabase.playlistDao()).thenReturn(playlistDao)
        whenever(playlistDao.updateBlocking(any())).then { }
        whenever(playlistUpdateAnalytics.update(any(), any(), any())).then { }

        return PlaylistManagerImpl(
            settings,
            downloadManager,
            playlistUpdateAnalytics,
            syncManager,
            notificationManager,
            context,
            appDatabase,
        )
    }
}
