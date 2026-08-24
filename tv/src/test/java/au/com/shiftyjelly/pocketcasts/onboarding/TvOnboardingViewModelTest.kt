package au.com.shiftyjelly.pocketcasts.onboarding

import android.content.Context
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class TvOnboardingViewModelTest {

    private val context = mock<Context>()
    private val syncManager = mock<SyncManager>()
    private val podcastManager = mock<PodcastManager>()
    private val settings = mock<Settings>()

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

    private fun viewModel() = TvOnboardingViewModel(context, syncManager, podcastManager, settings)
}
