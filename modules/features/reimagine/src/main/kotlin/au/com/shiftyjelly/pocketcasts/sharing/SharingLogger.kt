package au.com.shiftyjelly.pocketcasts.sharing

import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import timber.log.Timber

internal class SharingLogger : SharingClient.Listener {
    override fun onShare(request: SharingRequest) {
        val message = "Sharing: $request"
        Timber.tag(TAG).i(message)
        LogBuffer.i(TAG, message)
    }

    override fun onShared(request: SharingRequest, response: SharingResponse) {
        if (response.isSuccsessful) {
            Timber.tag(TAG).i("Shared $request")
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
