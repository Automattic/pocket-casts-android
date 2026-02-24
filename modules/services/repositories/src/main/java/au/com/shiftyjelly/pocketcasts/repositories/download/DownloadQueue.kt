package au.com.shiftyjelly.pocketcasts.repositories.download

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import kotlinx.coroutines.Deferred
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
    ): Deferred<Collection<BaseEpisode>>

    fun cancelAll(
        sourceView: SourceView,
    ): Deferred<Collection<BaseEpisode>>

    fun clearAllDownloadErrors(): Job
}

sealed interface DownloadType {
    data class UserTriggered(
        val waitForWifi: Boolean,
    ) : DownloadType

    data class Automatic(
        val bypassAutoDownloadStatus: Boolean,
    ) : DownloadType
}
