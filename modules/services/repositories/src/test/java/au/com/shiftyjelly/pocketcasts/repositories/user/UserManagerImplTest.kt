package au.com.shiftyjelly.pocketcasts.repositories.user

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class UserManagerImplTest {

    private val settings = mock<Settings>()
    private val syncManager = mock<SyncManager>()
    private val playbackManager = mock<PlaybackManager>()

    @Test
    fun `server-forced sign out emits onServerSignOut`() = runTest {
        whenever(settings.getFullySignedOut()).thenReturn(false)
        val userManager = createUserManager()

        userManager.onServerSignOut.test {
            userManager.signOut(playbackManager, wasInitiatedByUser = false)
            assertEquals(Unit, awaitItem())
        }
    }

    @Test
    fun `user-initiated sign out does not emit onServerSignOut`() = runTest {
        whenever(settings.getFullySignedOut()).thenReturn(false)
        val userManager = createUserManager()

        userManager.onServerSignOut.test {
            userManager.signOut(playbackManager, wasInitiatedByUser = true)
            expectNoEvents()
        }
    }

    @Test
    fun `forced sign out does not emit when already fully signed out`() = runTest {
        whenever(settings.getFullySignedOut()).thenReturn(true)
        val userManager = createUserManager()

        userManager.onServerSignOut.test {
            userManager.signOut(playbackManager, wasInitiatedByUser = false)
            expectNoEvents()
        }
    }

    private fun createUserManager() = UserManagerImpl(
        application = mock(),
        settings = settings,
        syncManager = syncManager,
        subscriptionManager = mock(),
        podcastManager = mock(),
        userEpisodeManager = mock(),
        playlistDao = mock(),
        playlistsInitializer = mock(),
        analyticsController = mock(),
        eventHorizon = mock(),
        accountStatusInfo = mock(),
        applicationScope = CoroutineScope(StandardTestDispatcher()),
        crashLogging = mock(),
        experimentProvider = mock(),
        endOfYearSync = mock(),
        notificationScheduler = mock(),
    )
}
