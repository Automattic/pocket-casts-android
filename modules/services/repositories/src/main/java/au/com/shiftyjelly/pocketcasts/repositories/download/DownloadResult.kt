package au.com.shiftyjelly.pocketcasts.repositories.download

import java.util.UUID

class DownloadResult private constructor(
    val jobId: UUID?,
    val success: Boolean,
    val episodeUuid: String,
    val errorMessage: String? = null
) {

    companion object {
        fun successResult(jobId: UUID, episodeUuid: String): DownloadResult {
            return DownloadResult(jobId = jobId, success = true, episodeUuid = episodeUuid)
        }

        fun failedResult(jobId: UUID?, errorMessage: String?, episodeUuid: String): DownloadResult {
            return DownloadResult(jobId = jobId, success = false, episodeUuid = episodeUuid, errorMessage = errorMessage)
        }
    }
}
