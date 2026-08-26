package au.com.shiftyjelly.pocketcasts.onboarding

import au.com.shiftyjelly.pocketcasts.auth.TvSignOutManager
import au.com.shiftyjelly.pocketcasts.onboarding.signedout.TvSignedOutViewModel
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SignedOutAlertShownEvent
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class TvSignedOutViewModelTest {

    private val eventHorizon = mock<EventHorizon>()
    private val signOutManager = mock<TvSignOutManager>()

    @Test
    fun `trackShown fires signed out alert shown event`() {
        viewModel().trackShown()

        verify(eventHorizon).track(SignedOutAlertShownEvent)
    }

    @Test
    fun `logOut wipes the local data`() {
        viewModel().logOut()

        verify(signOutManager).signOutAndWipeData()
    }

    private fun viewModel() = TvSignedOutViewModel(eventHorizon, signOutManager)
}
