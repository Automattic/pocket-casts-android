package au.com.shiftyjelly.pocketcasts.repositories.download

import au.com.shiftyjelly.pocketcasts.analytics.EpisodeDownloadError

class DownloadResult private constructor(
    val success: Boolean,
    val episodeUuid: String,
    val error: EpisodeDownloadError? = null,
    val errorMessage: String? = null,
) {

    companion object {
        fun successResult(episodeUuid: String): DownloadResult {
            return DownloadResult(success = true, episodeUuid = episodeUuid)
        }

        fun failedResult(episodeDownloadError: EpisodeDownloadError, errorMessage: String?): DownloadResult {
            return DownloadResult(success = false, episodeUuid = episodeDownloadError.episodeUuid, error = episodeDownloadError, errorMessage = errorMessage)
        }
    }
}
