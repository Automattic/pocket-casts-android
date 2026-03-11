package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.ViewModel
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.InformationalModalViewCardShowedEvent
import com.automattic.eventhorizon.InformationalModalViewDismissedEvent
import com.automattic.eventhorizon.InformationalModalViewGetStartedTapEvent
import com.automattic.eventhorizon.InformationalModalViewLoginTapEvent
import com.automattic.eventhorizon.InformationalModalViewShowedEvent
import com.automattic.eventhorizon.OnboardingBenefitType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingAccountBenefitsViewModel @Inject constructor(
    private val eventHorizon: EventHorizon,
) : ViewModel() {
    fun onScreenShown() {
        eventHorizon.track(InformationalModalViewShowedEvent)
    }

    fun onGetStartedClick() {
        eventHorizon.track(InformationalModalViewGetStartedTapEvent)
    }

    fun onLogInClick() {
        eventHorizon.track(InformationalModalViewLoginTapEvent)
    }

    fun onDismissClick() {
        eventHorizon.track(InformationalModalViewDismissedEvent)
    }

    fun onBenefitShown(benefitType: OnboardingBenefitType) {
        eventHorizon.track(
            InformationalModalViewCardShowedEvent(
                card = benefitType,
            ),
        )
    }
}
