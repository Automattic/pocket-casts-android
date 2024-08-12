package au.com.shiftyjelly.pocketcasts.analytics

import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class FirebaseAnalyticsTrackerTest {

    private lateinit var firebaseAnalytics: FirebaseAnalyticsWrapper
    private lateinit var tracker: FirebaseAnalyticsTracker

    companion object {
        private const val LIST_ID = "f255e707-1498-431e-8559-1e2c7125a561"
        private const val PODCAST_UUID = "4c4ada90-c44b-0136-7b94-27f978dac4db"
        private const val EPISODE_UUID = "4baff720-c0e6-496c-8967-f62b3f031cf0"
    }

    @Before
    fun setUp() {
        firebaseAnalytics = mock<FirebaseAnalyticsWrapper> {}
        tracker = FirebaseAnalyticsTracker(firebaseAnalytics = firebaseAnalytics)
    }

    @Test
    fun doNotTrackEventWhenShouldNotTrack() {
        tracker.track(AnalyticsEvent.UP_NEXT_SHOWN, emptyMap())

        verifyNoInteractions(firebaseAnalytics)
    }

    @Test
    fun trackEventListImpression() {
        tracker.track(
            event = AnalyticsEvent.DISCOVER_LIST_IMPRESSION,
            properties = mapOf("list_id" to LIST_ID),
        )

        val argBundle = argumentCaptor<Bundle>()
        verify(firebaseAnalytics).logEvent(eq("discover_list_impression"), argBundle.capture())
        assertBundle(argBundle.firstValue, listOf("list_id" to LIST_ID))
    }

    @Test
    fun trackEventListPodcastTap() {
        tracker.track(
            event = AnalyticsEvent.DISCOVER_LIST_PODCAST_TAPPED,
            properties = mapOf(
                "list_id" to LIST_ID,
                "podcast_uuid" to PODCAST_UUID,
            ),
        )

        val argBundle = argumentCaptor<Bundle>()
        verify(firebaseAnalytics).logEvent(eq("discover_list_podcast_tap"), argBundle.capture())
        assertBundle(argBundle.firstValue, listOf("list_id" to LIST_ID, "podcast_uuid" to PODCAST_UUID))
    }

    @Test
    fun trackEventListPodcastEpisodeTap() {
        tracker.track(
            event = AnalyticsEvent.DISCOVER_LIST_EPISODE_TAPPED,
            properties = mapOf(
                "list_id" to LIST_ID,
                "podcast_uuid" to PODCAST_UUID,
                "episode_uuid" to EPISODE_UUID,
            ),
        )

        val argBundle = argumentCaptor<Bundle>()
        verify(firebaseAnalytics).logEvent(eq("discover_list_podcast_episode_tap"), argBundle.capture())
        assertBundle(argBundle.firstValue, listOf("list_id" to LIST_ID, "podcast_uuid" to PODCAST_UUID, "episode_uuid" to EPISODE_UUID))
    }

    @Test
    fun trackEventListPodcastSubscribe() {
        tracker.track(
            event = AnalyticsEvent.DISCOVER_LIST_PODCAST_SUBSCRIBED,
            properties = mapOf(
                "list_id" to LIST_ID,
                "podcast_uuid" to PODCAST_UUID,
            ),
        )

        val argBundle = argumentCaptor<Bundle>()
        verify(firebaseAnalytics).logEvent(eq("discover_list_podcast_subscribe"), argBundle.capture())
        assertBundle(argBundle.firstValue, listOf("list_id" to LIST_ID, "podcast_uuid" to PODCAST_UUID))
    }

    @Test
    fun trackEventListShowAll() {
        tracker.track(
            event = AnalyticsEvent.DISCOVER_LIST_SHOW_ALL_TAPPED,
            properties = mapOf("list_id" to LIST_ID),
        )

        val argBundle = argumentCaptor<Bundle>()
        verify(firebaseAnalytics).logEvent(eq("discover_list_show_all"), argBundle.capture())
        assertBundle(argBundle.firstValue, listOf("list_id" to LIST_ID))
    }

    private fun assertBundle(bundle: Bundle, parameters: List<Pair<String, String>>) {
        assertEquals(bundle.size(), parameters.size)
        parameters.forEach { (key, value) ->
            assertEquals(bundle.getString(key), value)
        }
    }
}

// Analytic event discover_list_impression Bundle[{list_id=matt-ferrell}]
// Analytic event discover_list_impression Bundle[{list_id=}]
// Analytic event discover_list_impression Bundle[{list_id=f255e707-1498-431e-8559-1e2c7125a561}]
// Analytic event discover_list_impression Bundle[{list_id=464ac936-ce01-4554-9257-71bc36d9428a}]
// Analytic event discover_list_impression Bundle[{list_id=14dd6826-74d0-49d7-98ff-8b67f1976bb8}]
// Analytic event discover_list_podcast_tap Bundle[{podcast_uuid=4c4ada90-c44b-0136-7b94-27f978dac4db, list_id=f255e707-1498-431e-8559-1e2c7125a561}]
// Analytic event discover_list_podcast_subscribe Bundle[{podcast_uuid=4c4ada90-c44b-0136-7b94-27f978dac4db, list_id=f255e707-1498-431e-8559-1e2c7125a561}]
// Analytic event discover_list_podcast_episode_tap Bundle[{podcast_uuid=4c4ada90-c44b-0136-7b94-27f978dac4db, list_id=f255e707-1498-431e-8559-1e2c7125a561, episode_uuid=4baff720-c0e6-496c-8967-f62b3f031cf0}]
// Analytic event discover_list_episode_play Bundle[{podcast_id=4c4ada90-c44b-0136-7b94-27f978dac4db, list_id=f255e707-1498-431e-8559-1e2c7125a561}]
// Analytic event discover_list_show_all Bundle[{list_id=f255e707-1498-431e-8559-1e2c7125a561}]
// Analytic event discover_list_impression Bundle[{list_id=f255e707-1498-431e-8559-1e2c7125a561}]
//
//
// discover_list_podcast_tap with parameters “list_id” and “podcast_uuid”. When a podcast in a discover list is tapped.
// discover_list_podcast_subscribe with parameters “list_id” and “podcast_uuid”. When a podcast tapped on from the list is then subscribed to.
// discover_list_episode_play with parameters “list_id” and “podcast_uuid”. When an episodes is played after opening the podcast from the discover list.
// discover_list_impression with the parameter “list_id”. When the user views the list on the screen (it must be visible to the user).
