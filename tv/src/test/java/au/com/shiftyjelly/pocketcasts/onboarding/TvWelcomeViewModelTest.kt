package au.com.shiftyjelly.pocketcasts.onboarding

import au.com.shiftyjelly.pocketcasts.onboarding.welcome.TvWelcomeViewModel
import com.automattic.eventhorizon.BrowseNoAccountTappedEvent
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.OnboardingFlowType
import com.automattic.eventhorizon.SetupAccountButtonTappedEvent
import com.automattic.eventhorizon.SetupAccountButtonType
import com.automattic.eventhorizon.SetupAccountShownEvent
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class TvWelcomeViewModelTest {

    private val eventHorizon = mock<EventHorizon>()
    private val viewModel = TvWelcomeViewModel(eventHorizon)

    @Test
    fun `trackShown fires setup account shown event`() {
        viewModel.trackShown()

        verify(eventHorizon).track(SetupAccountShownEvent(flow = OnboardingFlowType.Unknown))
    }

    @Test
    fun `trackSignInTapped fires setup account button tapped with sign in`() {
        viewModel.trackSignInTapped()

        verify(eventHorizon).track(
            SetupAccountButtonTappedEvent(
                flow = OnboardingFlowType.Unknown,
                button = SetupAccountButtonType.SignIn,
            ),
        )
    }

    @Test
    fun `trackCreateAccountTapped fires setup account button tapped with create account`() {
        viewModel.trackCreateAccountTapped()

        verify(eventHorizon).track(
            SetupAccountButtonTappedEvent(
                flow = OnboardingFlowType.Unknown,
                button = SetupAccountButtonType.CreateAccount,
            ),
        )
    }

    @Test
    fun `trackBrowseNoAccountTapped fires browse no account tapped event`() {
        viewModel.trackBrowseNoAccountTapped()

        verify(eventHorizon).track(BrowseNoAccountTappedEvent)
    }
}
