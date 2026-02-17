package au.com.shiftyjelly.pocketcasts.repositories.download

interface DownloadQueue {
    fun enqueue(episodeUuid: String, downloadType: DownloadType) = enqueueAll(setOf(episodeUuid), downloadType)

    fun enqueueAll(episodeUuids: Collection<String>, downloadType: DownloadType)

    fun cancel(episodeUuid: String, disableAutoDownload: Boolean) = cancelAll(setOf(episodeUuid), disableAutoDownload)

    fun cancelAll(episodeUuids: Collection<String>, disableAutoDownload: Boolean)

    fun cancelAll(podcastUuid: String, disableAutoDownload: Boolean)
}

enum class DownloadType {
    UserTriggered,
    Automatic,
}
