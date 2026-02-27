package au.com.shiftyjelly.pocketcasts.sharing

import com.automattic.eventhorizon.EventHorizon

internal class SharingAnalytics(
    private val eventHorizon: EventHorizon,
) : SharingClient.Listener {
    override fun onShare(request: SharingRequest) {
        eventHorizon.track(request.trackable)
    }
}
