package au.com.shiftyjelly.pocketcasts.onboarding

import android.content.Context
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TvOnboardingViewModelTest {

    private val context = mock<Context>()
    private val syncManager = mock<SyncManager>()
    private val podcastManager = mock<PodcastManager>()
    private val settings = mock<Settings>()
    private val serverSignOut = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val userManager = mock<UserManager> { on { onServerSignOut } doReturn serverSignOut }

    @Test
    fun `start destination is landing when signed out`() {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        assertEquals(TvOnboardingRoutes.LANDING, viewModel().startDestination)
    }

    @Test
    fun `start destination is home when signed in and not fully signed out`() {
        whenever(syncManager.isLoggedIn()).thenReturn(true)
        whenever(settings.getFullySignedOut()).thenReturn(false)
        assertEquals(TvOnboardingRoutes.HOME, viewModel().startDestination)
    }

    @Test
    fun `start destination is landing when logged in but sign-out is pending`() {
        whenever(syncManager.isLoggedIn()).thenReturn(true)
        whenever(settings.getFullySignedOut()).thenReturn(true)
        assertEquals(TvOnboardingRoutes.LANDING, viewModel().startDestination)
    }

    @Test
    fun `re-emits the server sign out signal`() = runTest {
        viewModel().onServerSignOut.test {
            serverSignOut.emit(Unit)
            assertEquals(Unit, awaitItem())
        }
    }

    private fun viewModel() = TvOnboardingViewModel(context, syncManager, podcastManager, userManager, settings)
}
