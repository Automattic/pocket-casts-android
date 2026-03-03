package au.com.shiftyjelly.pocketcasts.analytics.testing

import com.automattic.eventhorizon.Trackable
import java.util.concurrent.ConcurrentLinkedQueue

class TestEventSink : (Trackable) -> Unit {
    private val events = ConcurrentLinkedQueue<Trackable>()

    override fun invoke(event: Trackable) {
        events += event
    }

    fun pollEvent(): Trackable {
        return checkNotNull(events.poll()) {
            "No events were found in the queue."
        }
    }

    fun skipEvent(count: Int = 1) {
        repeat(count) {
            pollEvent()
        }
    }

    fun isEmpty() = events.isEmpty()
}
