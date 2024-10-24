package au.com.shiftyjelly.pocketcasts.endofyear

import au.com.shiftyjelly.pocketcasts.sharing.SharingClient
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.sharing.SharingResponse

interface StorySharingClient {
    suspend fun shareStory(request: SharingRequest): SharingResponse
}

internal fun SharingClient.asStoryClient() = object : StorySharingClient {
    override suspend fun shareStory(request: SharingRequest): SharingResponse {
        return this@asStoryClient.share(request)
    }
}
