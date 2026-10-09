package au.com.shiftyjelly.pocketcasts.repositories.user

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.whatsnew.WhatsNewManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class UserManagerImplTest {

    private val settings = mock<Settings>()
    private val syncManager = mock<SyncManager>()
    private val playbackManager = mock<PlaybackManager>()
    private val whatsNewManager = mock<WhatsNewManager>()

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

    @Test
    fun `signing out forgets which what's new messages were read`() = runTest {
        whenever(settings.getFullySignedOut()).thenReturn(false)
        syncManager.stub {
            on { signOut(any()) } doSuspendableAnswer { invocation ->
                runCatching { invocation.getArgument<suspend () -> Unit>(0).invoke() }.getOrNull()
            }
        }
        val userManager = createUserManager()

        userManager.signOut(playbackManager, wasInitiatedByUser = true)
        advanceUntilIdle()

        verify(whatsNewManager).forgetReadMessages()
    }

    @Test
    fun `signing out turns the what's new unread dot back on`() = runTest {
        val showWhatsNewDot = mock<UserSetting<Boolean>>()
        whenever(settings.showWhatsNewDot).thenReturn(showWhatsNewDot)
        whenever(settings.getFullySignedOut()).thenReturn(false)
        syncManager.stub {
            on { signOut(any()) } doSuspendableAnswer { invocation ->
                runCatching { invocation.getArgument<suspend () -> Unit>(0).invoke() }.getOrNull()
            }
        }
        val userManager = createUserManager()

        userManager.signOut(playbackManager, wasInitiatedByUser = true)
        advanceUntilIdle()

        verify(showWhatsNewDot).reset()
    }

    private fun TestScope.createUserManager() = UserManagerImpl(
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
        applicationScope = CoroutineScope(StandardTestDispatcher(testScheduler)),
        crashLogging = mock(),
        experimentProvider = mock(),
        endOfYearSync = mock(),
        notificationScheduler = mock(),
        whatsNewManager = { whatsNewManager },
    )
}
