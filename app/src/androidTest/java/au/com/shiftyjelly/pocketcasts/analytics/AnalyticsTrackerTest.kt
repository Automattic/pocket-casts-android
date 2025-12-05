package au.com.shiftyjelly.pocketcasts.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsTrackerTest {
    private val testTracker = FakeTracker()
    private val analyticsTracker = AnalyticsTracker.test(testTracker)

    @Test
    fun trackBannerAdImpression() {
        analyticsTracker.trackBannerAdImpression(id = "test_id", location = "test_location")

        val event = testTracker.events.single()
        event.assertType(AnalyticsEvent.BANNER_AD_IMPRESSION)
        event.assertProperties(
            mapOf(
                "id" to "test_id",
                "location" to "test_location",
            ),
        )
    }

    @Test
    fun trackBannerAdTapped() {
        analyticsTracker.trackBannerAdTapped(id = "test_id", location = "test_location")

        val event = testTracker.events.single()
        event.assertType(AnalyticsEvent.BANNER_AD_TAPPED)
        event.assertProperties(
            mapOf(
                "id" to "test_id",
                "location" to "test_location",
            ),
        )
    }

    @Test
    fun trackBannerAdReport() {
        analyticsTracker.trackBannerAdReport(id = "test_id", reason = "test_reason", location = "test_location")

        val event = testTracker.events.single()
        event.assertType(AnalyticsEvent.BANNER_AD_REPORT)
        event.assertProperties(
            mapOf(
                "id" to "test_id",
                "reason" to "test_reason",
                "location" to "test_location",
            ),
        )
    }
}

private class FakeTracker : Tracker {
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

private fun TrackedEvent.assertType(type: AnalyticsEvent) {
    assertEquals(type, this.key)
}

private fun TrackedEvent.assertProperties(properties: Map<String, Any>) {
    assertEquals(properties, this.properties)
}
