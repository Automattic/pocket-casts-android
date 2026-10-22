package au.com.shiftyjelly.pocketcasts.onboarding

import au.com.shiftyjelly.pocketcasts.onboarding.createaccount.TvCreateAccountModalViewModel
import com.automattic.eventhorizon.CreateAccountShownEvent
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.OnboardingFlowType
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class TvCreateAccountModalViewModelTest {

    private val eventHorizon = mock<EventHorizon>()

    @Test
    fun `trackShown fires create account shown with the account encouragement flow`() {
        TvCreateAccountModalViewModel(eventHorizon).trackShown()

        verify(eventHorizon).track(CreateAccountShownEvent(flow = OnboardingFlowType.AccountEncouragement))
    }
}
