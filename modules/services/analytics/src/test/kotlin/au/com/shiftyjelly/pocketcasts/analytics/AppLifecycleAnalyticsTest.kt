package au.com.shiftyjelly.pocketcasts.analytics

import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import com.automattic.eventhorizon.ApplicationClosedEvent
import com.automattic.eventhorizon.ApplicationInstalledEvent
import com.automattic.eventhorizon.ApplicationOpenedEvent
import com.automattic.eventhorizon.ApplicationUpdatedEvent
import com.automattic.eventhorizon.EventHorizon
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLifecycleAnalyticsTest {
    private val eventSink = TestEventSink()
    private val timeSource = TestTimeSource()

    private val lifecycleAnalytics = AppLifecycleAnalytics(
        eventHorizon = EventHorizon(eventSink),
        timeSource = timeSource,
    )

    @Test
    fun `when app is installed, then installed event fired`() {
        lifecycleAnalytics.onNewApplicationInstall()

        assertEquals(ApplicationInstalledEvent, eventSink.pollEvent())
    }

    @Test
    fun `when app is updated, then updated event fired`() {
        lifecycleAnalytics.onApplicationUpgrade(previousVersionCode = 150)

        assertEquals(ApplicationUpdatedEvent(previousVersion = "150"), eventSink.pollEvent())
    }

    @Test
    fun `when app is foregrounded, then app opened event fired`() {
        lifecycleAnalytics.onApplicationEnterForeground()

        assertEquals(ApplicationOpenedEvent, eventSink.pollEvent())
    }

    @Test
    fun `when app is backgrounded, then app closed event fired with time in app`() {
        timeSource += 1.seconds
        lifecycleAnalytics.onApplicationEnterBackground()

        assertEquals(ApplicationClosedEvent(timeInApp = 1), eventSink.pollEvent())
    }
}
