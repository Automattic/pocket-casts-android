package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RequirePlusViewModel @Inject constructor(
    private val analytTracker: AnalyticsTracker,
) : ViewModel() {

    fun onShown() {
        analytTracker.track(AnalyticsEvent.WEAR_REQUIRE_PLUS_SHOWN)
    }
}
