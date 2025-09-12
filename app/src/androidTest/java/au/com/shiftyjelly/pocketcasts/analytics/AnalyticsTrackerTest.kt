package au.com.shiftyjelly.pocketcasts.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsTrackerTest {
    private val testTracker = TestTracker()
    private val analyticsTracker = AnalyticsTracker.test(testTracker, isFirstPartyEnabled = true)

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

    private class TestTracker : Tracker {
        private val _events = mutableListOf<TrackEvent>()

        val events get() = _events.toList()

        override fun track(event: AnalyticsEvent, properties: Map<String, Any>) {
            _events += TrackEvent(event, properties)
        }

        override fun getTrackerType() = TrackerType.FirstParty

        override fun refreshMetadata() = Unit

        override fun flush() = Unit

        override fun clearAllData() = Unit
    }

    private data class TrackEvent(
        val type: AnalyticsEvent,
        val properties: Map<String, Any>,
    ) {
        fun assertType(type: AnalyticsEvent) {
            assertEquals(type, this.type)
        }

        fun assertProperties(properties: Map<String, Any>) {
            assertEquals(properties, this.properties)
        }
    }
}
