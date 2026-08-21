package au.com.shiftyjelly.pocketcasts.onboarding

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TvOnboardingViewModel @Inject constructor(
    syncManager: SyncManager,
    settings: Settings,
) : ViewModel() {
    val startDestination: String = if (syncManager.isLoggedIn() && !settings.getFullySignedOut()) {
        TvOnboardingRoutes.HOME
    } else {
        TvOnboardingRoutes.LANDING
    }
}
