package au.com.shiftyjelly.pocketcasts.analytics

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpisodeAnalytics @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
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

    fun trackEvent(event: AnalyticsEvent, uuid: String) {
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

        analyticsTracker.track(event, AnalyticsProp.uuidMap(uuid))
    }

    fun trackEvent(
        event: AnalyticsEvent,
        source: SourceView,
        toTop: Boolean,
        episode: BaseEpisode,
    ) {
        analyticsTracker.track(
            event,
            AnalyticsProp.sourceAndToTopMap(source, toTop, episode)
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

    private object AnalyticsProp {
        private const val source = "source"
        private const val episode_uuid = "episode_uuid"
        private const val count = "count"
        private const val to_top = "to_top"
        fun sourceAndUuidMap(eventSource: SourceView, uuid: String) =
            mapOf(source to eventSource.analyticsValue, episode_uuid to uuid)

        fun uuidMap(uuid: String) = mapOf(episode_uuid to uuid)
        fun sourceAndToTopMap(eventSource: SourceView, toTop: Boolean, episode: BaseEpisode) =
            mapOf(
                source to eventSource.analyticsValue,
                to_top to toTop,
                episode_uuid to episode.uuid,
            )

        fun bulkMap(eventSource: SourceView, count: Int) =
            mapOf(source to eventSource.analyticsValue, this.count to count)

        fun bulkToTopMap(eventSource: SourceView, count: Int, toTop: Boolean) = mapOf(
            source to eventSource.analyticsValue,
            this.count to count,
            to_top to toTop
        )
    }
}
