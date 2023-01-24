package au.com.shiftyjelly.pocketcasts.analytics

import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpisodeAnalytics @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
) {
    private val downloadEpisodeUuidQueue = mutableListOf<String>()
    private val uploadEpisodeUuidQueue = mutableListOf<String>()

    fun trackEvent(event: AnalyticsEvent, source: AnalyticsSource, uuid: String) {
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

    fun trackEvent(event: AnalyticsEvent, source: AnalyticsSource, toTop: Boolean) {
        analyticsTracker.track(event, AnalyticsProp.sourceAndToTopMap(source, toTop))
    }

    fun trackBulkEvent(event: AnalyticsEvent, source: AnalyticsSource, count: Int) {
        analyticsTracker.track(event, AnalyticsProp.bulkMap(source, count))
    }

    fun trackBulkEvent(event: AnalyticsEvent, source: AnalyticsSource, episodes: List<Playable>) {
        if (event == AnalyticsEvent.EPISODE_BULK_DOWNLOAD_QUEUED) {
            downloadEpisodeUuidQueue.clear()
            downloadEpisodeUuidQueue.addAll(downloadEpisodeUuidQueue.union(episodes.map { it.uuid }))
        }
        analyticsTracker.track(event, AnalyticsProp.bulkMap(source, episodes.size))
    }

    fun trackBulkEvent(
        event: AnalyticsEvent,
        source: AnalyticsSource,
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
        fun sourceAndUuidMap(eventSource: AnalyticsSource, uuid: String) =
            mapOf(source to eventSource.analyticsValue, episode_uuid to uuid)

        fun uuidMap(uuid: String) = mapOf(episode_uuid to uuid)
        fun sourceAndToTopMap(eventSource: AnalyticsSource, toTop: Boolean) =
            mapOf(source to eventSource.analyticsValue, to_top to toTop)

        fun bulkMap(eventSource: AnalyticsSource, count: Int) =
            mapOf(source to eventSource.analyticsValue, this.count to count)

        fun bulkToTopMap(eventSource: AnalyticsSource, count: Int, toTop: Boolean) = mapOf(
            source to eventSource.analyticsValue,
            this.count to count,
            to_top to toTop
        )
    }
}
