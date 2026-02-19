package au.com.shiftyjelly.pocketcasts.repositories.download

import au.com.shiftyjelly.pocketcasts.analytics.SourceView

interface DownloadQueue {
    fun enqueue(
        episodeUuid: String,
        downloadType: DownloadType,
        sourceView: SourceView,
    ) = enqueueAll(setOf(episodeUuid), downloadType, sourceView)

    fun enqueueAll(
        episodeUuids: Collection<String>,
        downloadType: DownloadType,
        sourceView: SourceView,
    )

    fun cancel(
        episodeUuid: String,
        disableAutoDownload: Boolean,
        sourceView: SourceView,
    ) = cancelAll(setOf(episodeUuid), disableAutoDownload, sourceView)

    fun cancelAll(
        episodeUuids: Collection<String>,
        disableAutoDownload: Boolean,
        sourceView: SourceView,
    )

    fun cancelAll(
        podcastUuid: String,
        disableAutoDownload: Boolean,
        sourceView: SourceView,
    )
}

enum class DownloadType {
    UserTriggered,
    Automatic,
}
