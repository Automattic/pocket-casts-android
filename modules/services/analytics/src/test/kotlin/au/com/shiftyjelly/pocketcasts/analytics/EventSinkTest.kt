package au.com.shiftyjelly.pocketcasts.analytics

import com.automattic.eventhorizon.ApplicationInstalledEvent
import com.automattic.eventhorizon.ApplicationOpenedEvent
import com.automattic.eventhorizon.ApplicationUpdatedEvent
import com.automattic.eventhorizon.Trackable
import org.junit.Assert.assertEquals
import org.junit.Test

class EventSinkTest {
    private val testTracker = TestTracker()
    private val testListener = TestListener()

    private val eventSink = EventSink(
        trackers = setOf(testTracker),
        listeners = setOf(testListener),
    )

    @Test
    fun `dispatches events to trackers`() {
        testTracker.isTrackingEnabled = true
        eventSink(ApplicationInstalledEvent)
        eventSink(ApplicationOpenedEvent)

        testTracker.isTrackingEnabled = false
        eventSink(ApplicationUpdatedEvent(previousVersion = "1.0"))

        assertEquals(
            listOf(ApplicationInstalledEvent, ApplicationOpenedEvent),
            testTracker.events,
        )
    }

    @Test
    fun `dispatches events to listeners`() {
        eventSink(ApplicationInstalledEvent)
        eventSink(ApplicationOpenedEvent)

        assertEquals(
            listOf(ApplicationInstalledEvent, ApplicationOpenedEvent),
            testListener.events,
        )
    }
}

private class TestTracker : AnalyticsTracker {
    var isTrackingEnabled = true

    private val _events = mutableListOf<Trackable>()
    val events get() = _events.toList()

    override val id get() = "test-id"

    override fun track(event: Trackable): TrackedEvent? {
        return if (isTrackingEnabled) {
            _events.add(event)
            TrackedEvent(event.analyticsName, event.analyticsProperties)
        } else {
            null
        }
    }
}

private class TestListener : AnalyticsListener {
    private val _events = mutableListOf<Trackable>()
    val events get() = _events.toList()

    override fun onEvent(
        event: Trackable,
        trackedEvents: Map<String, TrackedEvent?>,
    ) {
        _events.add(event)
    }
}
