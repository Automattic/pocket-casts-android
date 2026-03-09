package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.OnboardingImportAppSelectedEvent
import com.automattic.eventhorizon.OnboardingImportDismissedEvent
import com.automattic.eventhorizon.OnboardingImportOpenAppSelectedEvent
import com.automattic.eventhorizon.OnboardingImportShownEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingImportViewModel @Inject constructor(
    private val eventHorizon: EventHorizon,
) : ViewModel() {

    fun onImportStartPageShown(flow: OnboardingFlow) {
        eventHorizon.track(
            OnboardingImportShownEvent(
                flow = flow.eventHorizonValue,
            ),
        )
    }

    fun onOpenApp(flow: OnboardingFlow, appName: String) {
        eventHorizon.track(
            OnboardingImportOpenAppSelectedEvent(
                flow = flow.eventHorizonValue,
                app = appName,
            ),
        )
    }

    fun onAppSelected(flow: OnboardingFlow, appName: String) {
        eventHorizon.track(
            OnboardingImportAppSelectedEvent(
                flow = flow.eventHorizonValue,
                app = appName,
            ),
        )
    }

    fun onImportDismissed(flow: OnboardingFlow) {
        eventHorizon.track(
            OnboardingImportDismissedEvent(
                flow = flow.eventHorizonValue,
            ),
        )
    }
}
