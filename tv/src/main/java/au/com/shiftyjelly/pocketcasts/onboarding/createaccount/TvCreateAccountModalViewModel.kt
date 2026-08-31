package au.com.shiftyjelly.pocketcasts.onboarding.createaccount

import androidx.lifecycle.ViewModel
import com.automattic.eventhorizon.CreateAccountShownEvent
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.OnboardingFlowType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TvCreateAccountModalViewModel @Inject constructor(
    private val eventHorizon: EventHorizon,
) : ViewModel() {

    fun trackShown() {
        eventHorizon.track(CreateAccountShownEvent(flow = OnboardingFlowType.AccountEncouragement))
    }
}
