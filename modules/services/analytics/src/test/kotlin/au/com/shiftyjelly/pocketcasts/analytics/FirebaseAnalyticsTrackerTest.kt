package au.com.shiftyjelly.pocketcasts.analytics

import android.os.Bundle
import au.com.shiftyjelly.pocketcasts.preferences.ReadWriteSetting
import com.automattic.eventhorizon.DiscoverListEpisodeTappedEvent
import com.automattic.eventhorizon.DiscoverListImpressionEvent
import com.automattic.eventhorizon.DiscoverListPodcastSubscribedEvent
import com.automattic.eventhorizon.DiscoverListPodcastTappedEvent
import com.automattic.eventhorizon.DiscoverListShowAllTappedEvent
import com.automattic.eventhorizon.UpNextShownEvent
import com.automattic.eventhorizon.UpNextSourceType
import java.time.Clock
import java.time.Instant
import java.util.concurrent.LinkedBlockingQueue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FirebaseAnalyticsTrackerTest {
    companion object {
        private const val LIST_ID = "f255e707-1498-431e-8559-1e2c7125a561"
        private const val LIST_DATETIME = "datetime_value"
        private const val PODCAST_UUID = "4c4ada90-c44b-0136-7b94-27f978dac4db"
        private const val EPISODE_UUID = "4baff720-c0e6-496c-8967-f62b3f031cf0"
    }

    private val firebaseAnalytics = TestFirebaseWrapper()

    private val tracker = FirebaseAnalyticsTracker(
        firebaseAnalytics = firebaseAnalytics,
        settings = mock {
            on { collectAnalytics } doReturn TestSetting(true)
        },
    )

    @Test
    fun doNotTrackEventWhenShouldNotTrack() {
        tracker.track(
            UpNextShownEvent(
                source = UpNextSourceType.UpNextShortcut,
            ),
        )

        assertTrue(firebaseAnalytics.isEmpty())
    }

    @Test
    fun trackEventListImpression() {
        tracker.track(
            DiscoverListImpressionEvent(
                listId = LIST_ID,
            ),
        )

        val event = firebaseAnalytics.pollEvent()

        assertEquals("discover_list_impression", event.name)
        assertBundle(event.params!!, listOf("list_id" to LIST_ID))
    }

    @Test
    fun trackEventListPodcastTap() {
        tracker.track(
            DiscoverListPodcastTappedEvent(
                listId = LIST_ID,
                podcastUuid = PODCAST_UUID,
            ),
        )

        val event = firebaseAnalytics.pollEvent()

        assertEquals("discover_list_podcast_tap", event.name)
        assertBundle(event.params!!, listOf("list_id" to LIST_ID, "podcast_uuid" to PODCAST_UUID))
    }

    @Test
    fun trackEventListPodcastEpisodeTap() {
        tracker.track(
            DiscoverListEpisodeTappedEvent(
                listId = LIST_ID,
                podcastUuid = PODCAST_UUID,
                episodeUuid = EPISODE_UUID,
            ),
        )

        val event = firebaseAnalytics.pollEvent()

        assertEquals("discover_list_podcast_episode_tap", event.name)
        assertBundle(event.params!!, listOf("list_id" to LIST_ID, "podcast_uuid" to PODCAST_UUID, "episode_uuid" to EPISODE_UUID))
    }

    @Test
    fun trackEventListPodcastSubscribe() {
        tracker.track(
            DiscoverListPodcastSubscribedEvent(
                listId = LIST_ID,
                podcastUuid = PODCAST_UUID,
            ),
        )

        val event = firebaseAnalytics.pollEvent()

        assertEquals("discover_list_podcast_subscribe", event.name)
        assertBundle(event.params!!, listOf("list_id" to LIST_ID, "podcast_uuid" to PODCAST_UUID))
    }

    @Test
    fun trackEventListShowAll() {
        tracker.track(
            DiscoverListShowAllTappedEvent(
                listId = LIST_ID,
                listDatetime = LIST_DATETIME,
            ),
        )

        val event = firebaseAnalytics.pollEvent()

        assertEquals("discover_list_show_all", event.name)
        assertBundle(event.params!!, listOf("list_id" to LIST_ID, "list_datetime" to LIST_DATETIME))
    }

    private fun assertBundle(bundle: Bundle, parameters: List<Pair<String, String>>) {
        assertEquals(bundle.size(), parameters.size)
        parameters.forEach { (key, value) ->
            assertEquals(bundle.getString(key), value)
        }
    }
}

private class TestFirebaseWrapper :
    FirebaseAnalyticsWrapper(
        firebaseAnalytics = mock(),
    ) {
    private val events = LinkedBlockingQueue<FirebaseEvent>()

    fun isEmpty() = events.isEmpty()

    fun pollEvent(): FirebaseEvent {
        return checkNotNull(events.poll()) {
            "No events were found in the queue."
        }
    }

    override fun logEvent(name: String, params: Bundle?) {
        events.add(FirebaseEvent(name, params))
    }
}

private data class FirebaseEvent(
    val name: String,
    val params: Bundle?,
)

private class TestSetting<T>(
    initialValue: T,
) : ReadWriteSetting<T> {
    private val stateFlow = MutableStateFlow(initialValue)

    override val value: T
        get() = stateFlow.value

    override val flow: StateFlow<T>
        get() = stateFlow

    override fun set(value: T, updateModifiedAt: Boolean, commit: Boolean, clock: Clock) {
        stateFlow.value = value
    }

    override fun getSyncValue(lastSyncTime: Instant): T? = value

    fun set(value: T) = set(value, updateModifiedAt = false)
}
