package au.com.shiftyjelly.pocketcasts.repositories.download

interface DownloadQueue {
    fun enqueue(episodeUuid: String, downloadType: DownloadType) = enqueueAll(setOf(episodeUuid), downloadType)

    fun enqueueAll(episodeUuids: Collection<String>, downloadType: DownloadType)
}

enum class DownloadType {
    UserTriggered,
    Automatic,
}
