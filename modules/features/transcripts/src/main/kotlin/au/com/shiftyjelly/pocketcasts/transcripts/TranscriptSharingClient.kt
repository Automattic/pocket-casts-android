package au.com.shiftyjelly.pocketcasts.transcripts

import au.com.shiftyjelly.pocketcasts.sharing.SharingClient
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.sharing.SharingResponse

interface TranscriptSharingClient {
    suspend fun shareTranscript(request: SharingRequest): SharingResponse
}

internal fun SharingClient.asTranscriptClient() = object : TranscriptSharingClient {
    override suspend fun shareTranscript(request: SharingRequest): SharingResponse {
        return this@asTranscriptClient.share(request)
    }
}
