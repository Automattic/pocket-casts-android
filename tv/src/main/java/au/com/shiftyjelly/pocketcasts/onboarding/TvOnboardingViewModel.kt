package au.com.shiftyjelly.pocketcasts.onboarding

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TvOnboardingViewModel @Inject constructor(
    settings: Settings,
) : ViewModel() {
    val startDestination: String = if (settings.hasCompletedOnboarding()) {
        TvOnboardingRoutes.HOME
    } else {
        TvOnboardingRoutes.LANDING
    }
}
