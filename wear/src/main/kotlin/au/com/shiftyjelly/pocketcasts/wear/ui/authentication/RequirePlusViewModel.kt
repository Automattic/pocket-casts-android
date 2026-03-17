package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.lifecycle.ViewModel
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.WearRequirePlusShownEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RequirePlusViewModel @Inject constructor(
    private val eventHorizon: EventHorizon,
) : ViewModel() {

    fun onShown() {
        eventHorizon.track(WearRequirePlusShownEvent)
    }
}
