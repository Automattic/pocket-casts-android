package au.com.shiftyjelly.pocketcasts.onboarding

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TvOnboardingViewModel @Inject constructor(
    syncManager: SyncManager,
) : ViewModel() {
    val startDestination: String = if (syncManager.isLoggedIn()) {
        TvOnboardingRoutes.HOME
    } else {
        TvOnboardingRoutes.LANDING
    }
}
