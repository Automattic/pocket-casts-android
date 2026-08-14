package au.com.shiftyjelly.pocketcasts.onboarding

import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class TvOnboardingViewModelTest {

    private val syncManager = mock<SyncManager>()

    @Test
    fun `start destination is landing when signed out`() {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        val viewModel = TvOnboardingViewModel(syncManager)
        assertEquals(TvOnboardingRoutes.LANDING, viewModel.startDestination)
    }

    @Test
    fun `start destination is home when signed in`() {
        whenever(syncManager.isLoggedIn()).thenReturn(true)
        val viewModel = TvOnboardingViewModel(syncManager)
        assertEquals(TvOnboardingRoutes.HOME, viewModel.startDestination)
    }
}
