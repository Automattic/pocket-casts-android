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
    fun `trackSignInClicked fires setup account button tapped with sign in`() {
        viewModel.trackSignInClicked()

        verify(eventHorizon).track(
            SetupAccountButtonTappedEvent(
                flow = OnboardingFlowType.Unknown,
                button = SetupAccountButtonType.SignIn,
            ),
        )
    }

    @Test
    fun `trackCreateAccountClicked fires setup account button tapped with create account`() {
        viewModel.trackCreateAccountClicked()

        verify(eventHorizon).track(
            SetupAccountButtonTappedEvent(
                flow = OnboardingFlowType.Unknown,
                button = SetupAccountButtonType.CreateAccount,
            ),
        )
    }

    @Test
    fun `trackBrowseWithoutAccountClicked fires browse no account tapped event`() {
        viewModel.trackBrowseWithoutAccountClicked()

        verify(eventHorizon).track(BrowseNoAccountTappedEvent)
    }
}
