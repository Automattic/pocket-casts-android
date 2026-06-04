package au.com.shiftyjelly.pocketcasts.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class TvOnboardingViewModelTest {

    @Test
    fun `start destination is landing`() {
        val viewModel = TvOnboardingViewModel()
        assertEquals(TvOnboardingRoutes.LANDING, viewModel.startDestination)
    }
}
