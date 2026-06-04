package au.com.shiftyjelly.pocketcasts.onboarding

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TvOnboardingViewModelTest {

    private val settings = mock<Settings>()

    @Test
    fun `start destination is landing when onboarding not completed`() {
        whenever(settings.hasCompletedOnboarding()).thenReturn(false)
        val viewModel = TvOnboardingViewModel(settings)
        assertEquals(TvOnboardingRoutes.LANDING, viewModel.startDestination)
    }

    @Test
    fun `start destination is home when onboarding completed`() {
        whenever(settings.hasCompletedOnboarding()).thenReturn(true)
        val viewModel = TvOnboardingViewModel(settings)
        assertEquals(TvOnboardingRoutes.HOME, viewModel.startDestination)
    }

    @Test
    fun `complete onboarding persists to settings`() {
        val viewModel = TvOnboardingViewModel(settings)
        viewModel.completeOnboarding()
        verify(settings).setHasDoneInitialOnboarding()
    }
}
