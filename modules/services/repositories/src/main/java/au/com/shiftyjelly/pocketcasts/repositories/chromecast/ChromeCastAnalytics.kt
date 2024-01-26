package au.com.shiftyjelly.pocketcasts.repositories.chromecast

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ChromeCastAnalytics @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val castManager: CastManager,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    fun trackChromeCastViewShown() {
        applicationScope.launch {
            val isConnected = castManager.isConnected()
            analyticsTracker.track(
                AnalyticsEvent.CHROMECAST_VIEW_SHOWN,
                mapOf("is_connected" to isConnected),
            )
        }
    }
}
