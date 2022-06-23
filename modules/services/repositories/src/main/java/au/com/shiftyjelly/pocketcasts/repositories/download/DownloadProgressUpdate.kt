package au.com.shiftyjelly.pocketcasts.repositories.download

import android.os.Parcelable
import androidx.work.Data
import kotlinx.parcelize.Parcelize

@Parcelize
data class DownloadProgressUpdate(
    var episodeUuid: String,
    var podcastUuid: String? = null,
    var customMessage: String? = null,
    var downloadProgress: Float = 0f,
    var downloadedSoFar: Long = 0,
    var totalToDownload: Long = 0
) : Parcelable

fun DownloadProgressUpdate.toData(): Data {
    return Data.Builder()
        .putString("uuid", episodeUuid)
        .putString("podcastUuid", podcastUuid)
        .putString("customMessage", customMessage)
        .putFloat("downloadProgress", downloadProgress)
        .putLong("downloadedSoFar", downloadedSoFar)
        .putLong("totalDownload", totalToDownload)
        .build()
}

fun Data.toDownloadProgressUpdate(): DownloadProgressUpdate? {
    val uuid = getString("uuid") ?: return null
    return DownloadProgressUpdate(
        uuid,
        getString("podcastUuid"),
        getString("customMessage"),
        getFloat("downloadProgress", 0f),
        getLong("downloadedSoFar", 0),
        getLong("totalDownload", 0)
    )
}
