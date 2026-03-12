package au.com.shiftyjelly.pocketcasts.analytics

import com.automattic.eventhorizon.Trackable

class EventSink(
    private val trackers: Set<Tracker>,
    private val listeners: Set<AnalyticsListener>,
) : (Trackable) -> Unit,
    AnalyticsController {
    override fun invoke(event: Trackable) {
        val analyticsEvent = EVENT_MAP[event.analyticsName] ?: return
        val trackedEvents = trackers.associate { tracker ->
            val trackedEvent = if (tracker.shouldTrack(analyticsEvent)) {
                tracker.track(analyticsEvent, event.analyticsProperties)
            } else {
                null
            }
            tracker.id to trackedEvent
        }
        listeners.forEach { listener ->
            listener.onEvent(event, trackedEvents)
        }
    }

    override fun refreshMetadata() {
        trackers.forEach(Tracker::refreshMetadata)
    }

    override fun flush() {
        trackers.forEach(Tracker::flush)
    }

    override fun clearAllData() {
        trackers.forEach(Tracker::clearAllData)
    }
}

interface AnalyticsListener {
    fun onEvent(
        event: Trackable,
        trackedEvents: Map<String, TrackedEvent?> = emptyMap(),
    )
}

interface AnalyticsController {
    fun refreshMetadata()

    fun flush()

    fun clearAllData()
}

private val EVENT_MAP = AnalyticsEvent.entries.associateBy(AnalyticsEvent::key)
