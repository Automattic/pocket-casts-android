package au.com.shiftyjelly.pocketcasts.onboarding.welcome

import androidx.lifecycle.ViewModel
import com.automattic.eventhorizon.BrowseNoAccountTappedEvent
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.OnboardingFlowType
import com.automattic.eventhorizon.SetupAccountButtonTappedEvent
import com.automattic.eventhorizon.SetupAccountButtonType
import com.automattic.eventhorizon.SetupAccountShownEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TvWelcomeViewModel @Inject constructor(
    private val eventHorizon: EventHorizon,
) : ViewModel() {

    fun trackShown() {
        eventHorizon.track(SetupAccountShownEvent(flow = OnboardingFlowType.Unknown))
    }

    fun trackSignInTapped() {
        eventHorizon.track(
            SetupAccountButtonTappedEvent(
                flow = OnboardingFlowType.Unknown,
                button = SetupAccountButtonType.SignIn,
            ),
        )
    }

    fun trackCreateAccountTapped() {
        eventHorizon.track(
            SetupAccountButtonTappedEvent(
                flow = OnboardingFlowType.Unknown,
                button = SetupAccountButtonType.CreateAccount,
            ),
        )
    }

    fun trackBrowseNoAccountTapped() {
        eventHorizon.track(BrowseNoAccountTappedEvent)
    }
}
