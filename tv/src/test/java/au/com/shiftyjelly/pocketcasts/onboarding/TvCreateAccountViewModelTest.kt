package au.com.shiftyjelly.pocketcasts.onboarding

import au.com.shiftyjelly.pocketcasts.onboarding.createaccount.TvCreateAccountViewModel
import com.automattic.eventhorizon.CreateAccountShownEvent
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.OnboardingFlowType
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class TvCreateAccountViewModelTest {

    private val eventHorizon = mock<EventHorizon>()
    private val viewModel = TvCreateAccountViewModel(eventHorizon)

    @Test
    fun `trackShown fires create account shown event`() {
        viewModel.trackShown()

        verify(eventHorizon).track(CreateAccountShownEvent(flow = OnboardingFlowType.InitialOnboarding))
    }
}
