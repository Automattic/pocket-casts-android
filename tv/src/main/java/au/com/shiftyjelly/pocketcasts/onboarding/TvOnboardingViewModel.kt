package au.com.shiftyjelly.pocketcasts.onboarding

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TvOnboardingViewModel @Inject constructor() : ViewModel() {
    // TODO: Read Settings.hasCompletedOnboarding() once Firebase is configured for TV.
    //  Currently Settings pulls in FirebaseRemoteConfig which isn't initialized in the TV app.
    val startDestination: String = TvOnboardingRoutes.LANDING
}
