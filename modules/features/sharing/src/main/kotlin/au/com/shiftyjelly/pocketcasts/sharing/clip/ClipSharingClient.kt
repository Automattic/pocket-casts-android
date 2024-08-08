package au.com.shiftyjelly.pocketcasts.sharing.clip

import au.com.shiftyjelly.pocketcasts.sharing.SharingClient
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.sharing.SharingResponse

interface ClipSharingClient {
    suspend fun shareClip(request: SharingRequest): SharingResponse
}

internal fun SharingClient.asClipClient() = object : ClipSharingClient {
    override suspend fun shareClip(request: SharingRequest): SharingResponse {
        return this@asClipClient.share(request)
    }
}
