package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.utils.extensions.isGooglePlayServicesAvailableSuccess
import com.google.android.gms.common.GoogleApiAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoogleSignInState(val isNewAccount: Boolean)

@HiltViewModel
class OnboardingLoginOrSignUpViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    @ApplicationContext context: Context,
    private val podcastManager: PodcastManager,
) : AndroidViewModel(context as Application) {

    val showContinueWithGoogleButton =
        Settings.GOOGLE_SIGN_IN_SERVER_CLIENT_ID.isNotEmpty() &&
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailableSuccess(context)

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    sealed class UiState {
        object Loading : UiState()
        data class Loaded(val randomPodcasts: List<Podcast>) : UiState()
    }

    init {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val randomPodcasts = podcastManager.findRandomPodcasts(limit = 6)
            _uiState.value = UiState.Loaded(randomPodcasts = randomPodcasts)
        }
    }

    fun onShown(flow: OnboardingFlow) {
        analyticsTracker.track(
            AnalyticsEvent.SETUP_ACCOUNT_SHOWN,
            mapOf(AnalyticsProp.flow(flow))
        )
    }

    fun onDismiss(flow: OnboardingFlow) {
        analyticsTracker.track(
            AnalyticsEvent.SETUP_ACCOUNT_DISMISSED,
            mapOf(AnalyticsProp.flow(flow))
        )
    }

    fun onSignUpClicked(flow: OnboardingFlow) {
        analyticsTracker.track(
            AnalyticsEvent.SETUP_ACCOUNT_BUTTON_TAPPED,
            mapOf(AnalyticsProp.flow(flow), AnalyticsProp.ButtonTapped.createAccount)
        )
    }

    fun onLoginClicked(flow: OnboardingFlow) {
        analyticsTracker.track(
            AnalyticsEvent.SETUP_ACCOUNT_BUTTON_TAPPED,
            mapOf(AnalyticsProp.flow(flow), AnalyticsProp.ButtonTapped.signIn)
        )
    }

    companion object {
        object AnalyticsProp {
            fun flow(flow: OnboardingFlow) = "flow" to flow.analyticsValue
            object ButtonTapped {
                private const val button = "button"
                val signIn = button to "sign_in"
                val createAccount = button to "create_account"
                val continueWithGoogle = button to "continue_with_google"
            }
        }
    }
}
