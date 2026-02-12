package au.com.shiftyjelly.pocketcasts.repositories.download

interface DownloadQueue {
    fun enqueue(episodeUuid: String, downloadType: DownloadType) = enqueueAll(setOf(episodeUuid), downloadType)

    fun enqueueAll(episodeUuids: Collection<String>, downloadType: DownloadType)

    fun cancel(episodeUuid: String) = cancelAll(setOf(episodeUuid))

    fun cancelAll(episodeUuids: Collection<String>)

    fun cancelAll(podcastUuid: String)
}

enum class DownloadType {
    UserTriggered,
    Automatic,
}
