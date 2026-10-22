package au.com.shiftyjelly.pocketcasts.onboarding.signedout

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.auth.TvSignOutManager
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SignedOutAlertShownEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TvSignedOutViewModel @Inject constructor(
    private val eventHorizon: EventHorizon,
    private val signOutManager: TvSignOutManager,
) : ViewModel() {

    fun trackShown() {
        eventHorizon.track(SignedOutAlertShownEvent)
    }

    fun logOut() {
        signOutManager.signOutAndWipeData(wasInitiatedByUser = false)
    }
}
