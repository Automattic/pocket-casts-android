package au.com.shiftyjelly.pocketcasts.reimagine

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.TrackedEvent
import au.com.shiftyjelly.pocketcasts.analytics.Tracker

class FakeTracker : Tracker {
    private val _events = mutableListOf<TrackedEvent>()

    val events get() = _events.toList()

    override val id get() = "fake_tracker"

    override fun shouldTrack(event: AnalyticsEvent) = true

    override fun track(event: AnalyticsEvent, properties: Map<String, Any>): TrackedEvent {
        val trackedEvent = TrackedEvent(event, properties)
        _events += trackedEvent
        return trackedEvent
    }

    override fun refreshMetadata() = Unit

    override fun flush() = Unit

    override fun clearAllData() = Unit
}
