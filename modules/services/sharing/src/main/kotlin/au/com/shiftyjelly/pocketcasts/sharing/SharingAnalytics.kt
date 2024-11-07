package au.com.shiftyjelly.pocketcasts.sharing

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker

internal class SharingAnalytics(
    private val tracker: AnalyticsTracker,
) : SharingClient.Listener {
    override fun onShare(request: SharingRequest) {
        request.analyticsEvent?.let { event ->
            tracker.track(event, request.analyticsProperties)
        }
    }
}
