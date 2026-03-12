package au.com.shiftyjelly.pocketcasts.analytics

import com.automattic.eventhorizon.ApplicationInstalledEvent
import com.automattic.eventhorizon.ApplicationOpenedEvent
import com.automattic.eventhorizon.Trackable
import org.junit.Assert.assertEquals
import org.junit.Test

class EventSinkTest {
    private val testListener = TestListener()

    private val eventSink = EventSink(
        trackers = emptySet(),
        listeners = setOf(testListener),
    )

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
