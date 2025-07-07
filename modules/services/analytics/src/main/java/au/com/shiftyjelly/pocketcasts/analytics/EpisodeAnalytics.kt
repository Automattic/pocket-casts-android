package au.com.shiftyjelly.pocketcasts.analytics

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeDownloadFailureStatistics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpisodeAnalytics @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
) {
    private val downloadEpisodeUuidQueue = mutableListOf<String>()
    private val uploadEpisodeUuidQueue = mutableListOf<String>()

    fun trackEvent(event: AnalyticsEvent, source: SourceView, uuid: String) {
        if (event == AnalyticsEvent.EPISODE_DOWNLOAD_QUEUED) {
            downloadEpisodeUuidQueue.add(uuid)
        } else if (event == AnalyticsEvent.EPISODE_UPLOAD_QUEUED) {
            uploadEpisodeUuidQueue.add(uuid)
        }
        analyticsTracker.track(event, AnalyticsProp.sourceAndUuidMap(source, uuid))
    }

    fun trackEvent(event: AnalyticsEvent, uuid: String, source: SourceView? = null) {
        if (event == AnalyticsEvent.EPISODE_DOWNLOAD_FINISHED || event == AnalyticsEvent.EPISODE_DOWNLOAD_FAILED) {
            if (downloadEpisodeUuidQueue.contains(uuid)) {
                downloadEpisodeUuidQueue.remove(uuid)
            } else {
                return
            }
        } else if (event == AnalyticsEvent.EPISODE_UPLOAD_FINISHED || event == AnalyticsEvent.EPISODE_UPLOAD_FAILED) {
            if (uploadEpisodeUuidQueue.contains(uuid)) {
                uploadEpisodeUuidQueue.remove(uuid)
            } else {
                return
            }
        }

        if (source != null) {
            analyticsTracker.track(event, AnalyticsProp.sourceAndUuidMap(source, uuid))
        } else {
            analyticsTracker.track(event, AnalyticsProp.uuidMap(uuid))
        }
    }

    fun trackEvent(
        event: AnalyticsEvent,
        source: SourceView,
        toTop: Boolean,
        episode: BaseEpisode,
    ) {
        analyticsTracker.track(
            event,
            AnalyticsProp.sourceAndToTopMap(source, toTop, episode),
        )
    }

    fun trackBulkEvent(event: AnalyticsEvent, source: SourceView, count: Int) {
        analyticsTracker.track(event, AnalyticsProp.bulkMap(source, count))
    }

    fun trackBulkEvent(event: AnalyticsEvent, source: SourceView, episodes: List<BaseEpisode>) {
        if (event == AnalyticsEvent.EPISODE_BULK_DOWNLOAD_QUEUED) {
            downloadEpisodeUuidQueue.clear()
            downloadEpisodeUuidQueue.addAll(downloadEpisodeUuidQueue.union(episodes.map { it.uuid }))
        }
        analyticsTracker.track(event, AnalyticsProp.bulkMap(source, episodes.size))
    }

    fun trackBulkEvent(
        event: AnalyticsEvent,
        source: SourceView,
        count: Int,
        toTop: Boolean,
    ) {
        analyticsTracker.track(event, AnalyticsProp.bulkToTopMap(source, count, toTop))
    }

    fun trackEpisodeDownloadFailure(error: EpisodeDownloadError) {
        if (downloadEpisodeUuidQueue.contains(error.episodeUuid)) {
            downloadEpisodeUuidQueue.remove(error.episodeUuid)
        } else {
            return
        }
        analyticsTracker.track(AnalyticsEvent.EPISODE_DOWNLOAD_FAILED, error.toProperties())
    }

    fun trackStaleEpisodeDownloads(data: EpisodeDownloadFailureStatistics) {
        val properties = buildMap {
            put("failed_download_count", data.count)
            data.newestTimestamp?.let { put("newest_failed_download", it.toString()) }
            data.oldestTimestamp?.let { put("oldest_failed_download", it.toString()) }
        }
        analyticsTracker.track(AnalyticsEvent.EPISODE_DOWNLOAD_STALE, properties)
    }

    private object AnalyticsProp {
        private const val SOURCE = "source"
        private const val PODCAST_UUID = "podcast_uuid"
        private const val EPISODE_UUID = "episode_uuid"
        private const val COUNT = "count"
        private const val TO_TOP = "to_top"
        fun sourceAndUuidMap(eventSource: SourceView, uuid: String) = mapOf(SOURCE to eventSource.analyticsValue, EPISODE_UUID to uuid)

        fun uuidMap(uuid: String) = mapOf(EPISODE_UUID to uuid)
        fun sourceAndToTopMap(eventSource: SourceView, toTop: Boolean, episode: BaseEpisode) = mapOf(
            SOURCE to eventSource.analyticsValue,
            TO_TOP to toTop,
            PODCAST_UUID to episode.podcastOrSubstituteUuid,
            EPISODE_UUID to episode.uuid,
        )

        fun bulkMap(eventSource: SourceView, count: Int) = mapOf(SOURCE to eventSource.analyticsValue, this.COUNT to count)

        fun bulkToTopMap(eventSource: SourceView, count: Int, toTop: Boolean) = mapOf(
            SOURCE to eventSource.analyticsValue,
            this.COUNT to count,
            TO_TOP to toTop,
        )
    }
}
