package au.com.shiftyjelly.pocketcasts.sharing.clip

import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.sharing.SharingResponse
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

class FakeClipSharingClient : ClipSharingClient {
    var response = SharingResponse(
        isSuccsessful = true,
        feedbackMessage = null,
        error = null,
    )
    var request: SharingRequest? = null
        private set
    var isSuspended = false
        set(value) {
            field = value
            if (!value) {
                continueSignal.tryEmit(Unit)
            }
        }
    private val continueSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override suspend fun shareClip(request: SharingRequest): SharingResponse {
        if (isSuspended) {
            continueSignal.first()
        }
        this.request = request
        return response
    }
}
