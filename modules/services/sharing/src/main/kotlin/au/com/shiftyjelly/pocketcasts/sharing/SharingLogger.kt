package au.com.shiftyjelly.pocketcasts.sharing

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import timber.log.Timber

internal class SharingLogger : SharingClient.Listener {
    override fun onShare(
        request: SharingRequest,
        event: AnalyticsEvent,
        eventProperties: Map<String, Any>,
    ) {
        val message = "Sharing: $request $event $eventProperties"
        Timber.tag(TAG).i(message)
        LogBuffer.i(TAG, message)
    }

    override fun onShared(
        request: SharingRequest,
        response: SharingResponse,
        event: AnalyticsEvent,
        eventProperties: Map<String, Any>,
    ) {
        if (response.isSuccsessful) {
            Timber.tag(TAG).i("Shared $request $event $eventProperties")
        } else {
            val message = "Failed to share $request. Error message: ${response.feedbackMessage}"
            Timber.tag(TAG).e(response.error, message)
            if (response.error != null) {
                LogBuffer.e(TAG, response.error, message)
            } else {
                LogBuffer.e(TAG, message)
            }
        }
    }

    private companion object {
        const val TAG = "Sharing"
    }
}
