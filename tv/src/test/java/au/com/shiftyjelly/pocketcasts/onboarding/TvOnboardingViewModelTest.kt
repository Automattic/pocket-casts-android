package au.com.shiftyjelly.pocketcasts.onboarding

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class TvOnboardingViewModelTest {

    @Test
    fun `start destination is home when onboarding completed`() {
        val settings = mock<Settings>()
        whenever(settings.hasCompletedOnboarding()).thenReturn(true)

        val viewModel = TvOnboardingViewModel(settings)

        assertEquals(TvOnboardingRoutes.HOME, viewModel.startDestination)
    }

    @Test
    fun `start destination is landing when onboarding not completed`() {
        val settings = mock<Settings>()
        whenever(settings.hasCompletedOnboarding()).thenReturn(false)

        val viewModel = TvOnboardingViewModel(settings)

        assertEquals(TvOnboardingRoutes.LANDING, viewModel.startDestination)
    }
}
