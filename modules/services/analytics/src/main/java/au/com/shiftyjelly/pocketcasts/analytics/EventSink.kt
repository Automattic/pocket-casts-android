package au.com.shiftyjelly.pocketcasts.analytics

import com.automattic.eventhorizon.Trackable

class EventSink(
    private val trackers: Set<Tracker>,
    private val listeners: Set<AnalyticsTracker.Listener>,
) : (Trackable) -> Unit {
    override fun invoke(event: Trackable) {
        val analyticsEvent = EVENT_MAP[event.name] ?: return
        val trackedEvents = trackers.associate { tracker ->
            val trackedEvent = if (tracker.shouldTrack(analyticsEvent)) {
                tracker.track(analyticsEvent, event.properties)
            } else {
                null
            }
            tracker.id to trackedEvent
        }
        listeners.forEach { listener ->
            listener.onEvent(analyticsEvent, event.properties, trackedEvents)
        }
    }
}

private val EVENT_MAP = AnalyticsEvent.entries.associateBy(AnalyticsEvent::key)
