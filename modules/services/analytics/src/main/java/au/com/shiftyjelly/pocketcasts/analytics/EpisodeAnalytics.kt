package au.com.shiftyjelly.pocketcasts.analytics

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpisodeAnalytics @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
) {
    private val uploadEpisodeUuidQueue = mutableListOf<String>()

    fun trackEvent(event: AnalyticsEvent, source: AnalyticsSource, uuid: String) {
        if (event == AnalyticsEvent.EPISODE_UPLOAD_QUEUED) {
            uploadEpisodeUuidQueue.add(uuid)
        }
        analyticsTracker.track(event, AnalyticsProp.sourceAndUuidMap(source, uuid))
    }

    fun trackEvent(event: AnalyticsEvent, uuid: String) {
        if (event == AnalyticsEvent.EPISODE_UPLOAD_FINISHED) {
            if (uploadEpisodeUuidQueue.contains(uuid)) {
                uploadEpisodeUuidQueue.remove(uuid)
            } else {
                return
            }
        }

        analyticsTracker.track(event, AnalyticsProp.uuidMap(uuid))
    }

    private object AnalyticsProp {
        private const val source = "source"
        private const val episode_uuid = "episode_uuid"
        fun sourceAndUuidMap(eventSource: AnalyticsSource, uuid: String) = mapOf(source to eventSource.analyticsValue, episode_uuid to uuid)
        fun uuidMap(uuid: String) = mapOf(episode_uuid to uuid)
    }
}
