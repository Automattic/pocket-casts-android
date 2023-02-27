package au.com.shiftyjelly.pocketcasts.repositories.chromecast

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ChromeCastAnalytics @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val castManager: CastManager,
) {
    fun trackChromeCastViewShown() {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            val isConnected = castManager.isConnected()
            analyticsTracker.track(
                AnalyticsEvent.CHROMECAST_VIEW_SHOWN,
                mapOf("is_connected" to isConnected)
            )
        }
    }
}
