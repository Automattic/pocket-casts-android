package au.com.shiftyjelly.pocketcasts.repositories.download

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import kotlinx.coroutines.Job

interface DownloadQueue {
    val size: Int

    fun enqueue(
        episodeUuid: String,
        downloadType: DownloadType,
        sourceView: SourceView,
    ): Job = enqueueAll(setOf(episodeUuid), downloadType, sourceView)

    fun enqueueAll(
        episodeUuids: Collection<String>,
        downloadType: DownloadType,
        sourceView: SourceView,
    ): Job

    fun cancel(
        episodeUuid: String,
        sourceView: SourceView,
    ): Job = cancelAll(setOf(episodeUuid), sourceView)

    fun cancelAll(
        episodeUuids: Collection<String>,
        sourceView: SourceView,
    ): Job

    fun cancelAll(
        podcastUuid: String,
        sourceView: SourceView,
    ): Job

    fun cancelAll(
        sourceView: SourceView,
    ): Job

    fun clearAllDownloadErrors(): Job
}

sealed interface DownloadType {
    data class UserTriggered(
        val waitForWifi: Boolean,
    ) : DownloadType

    data object Automatic : DownloadType
}
