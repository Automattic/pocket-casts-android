package au.com.shiftyjelly.pocketcasts.sharing.clip

import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.sharing.SharingResponse

class FakeClipSharingClient : ClipSharingClient {
    var response = SharingResponse(
        isSuccsessful = true,
        feedbackMessage = null,
        error = null,
    )
    var request: SharingRequest? = null
        private set

    override suspend fun shareClip(request: SharingRequest): SharingResponse {
        this.request = request
        return response
    }
}
