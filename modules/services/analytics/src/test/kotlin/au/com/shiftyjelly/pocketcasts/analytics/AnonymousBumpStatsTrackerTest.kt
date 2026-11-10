package au.com.shiftyjelly.pocketcasts.analytics

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.BumpStatsDao
import au.com.shiftyjelly.pocketcasts.preferences.ReadWriteSetting
import com.automattic.eventhorizon.DiscoverListImpressionEvent
import com.automattic.eventhorizon.UpNextShownEvent
import com.automattic.eventhorizon.UpNextSourceType
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnonymousBumpStatsTrackerTest {
    private companion object {
        private const val LIST_ID = "f255e707-1498-431e-8559-1e2c7125a561"
    }

    private val bumpStatsDao = mock<BumpStatsDao>()
    private val appDatabase = mock<AppDatabase> {
        on { bumpStatsDao() } doReturn bumpStatsDao
    }

    private fun createTracker(collectAnalyticsEnabled: Boolean) = AnonymousBumpStatsTracker(
        appDatabase = appDatabase,
        settings = mock {
            on { collectAnalytics } doReturn FakeReadWriteSetting(collectAnalyticsEnabled)
        },
    )

    @Test
    fun `does not track when analytics collection is disabled`() {
        val tracker = createTracker(collectAnalyticsEnabled = false)

        assertNull(tracker.track(DiscoverListImpressionEvent(listId = LIST_ID)))
    }

    @Test
    fun `does not track events outside the paid sponsor list`() {
        val tracker = createTracker(collectAnalyticsEnabled = true)

        assertNull(tracker.track(UpNextShownEvent(source = UpNextSourceType.UpNextShortcut)))
    }

    @Test
    fun `tracks paid sponsor events with a bumped event name`() {
        val tracker = createTracker(collectAnalyticsEnabled = true)

        val trackedEvent = tracker.track(DiscoverListImpressionEvent(listId = LIST_ID))

        assertEquals("pcandroid_discover_list_impression_bump", trackedEvent?.key)
    }
}

private class FakeReadWriteSetting<T>(
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
}
