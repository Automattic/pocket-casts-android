package au.com.shiftyjelly.pocketcasts.shared

import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.Lifecycle.State.INITIALIZED
import androidx.lifecycle.testing.TestLifecycleOwner
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeDownloadFailureStatistics
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadStatisticsReporterTest {
    private val statistics = EpisodeDownloadFailureStatistics(
        count = 10,
        newestTimestamp = Instant.EPOCH.plusSeconds(15),
        oldestTimestamp = Instant.EPOCH,
    )

    private val testDispatcher = UnconfinedTestDispatcher()
    private val tracker = TestTracker()
    private val lifecycleOwner = TestLifecycleOwner(
        initialState = INITIALIZED,
        coroutineDispatcher = testDispatcher,
    )
    private lateinit var episodeDao: EpisodeDao
    private lateinit var reporter: DownloadStatisticsReporter

    @Before
    fun setUp() {
        episodeDao = mock<EpisodeDao> {
            onBlocking { getFailedDownloadsStatistics() } doReturn statistics
        }
        reporter = DownloadStatisticsReporter(
            episodeDao,
            EpisodeAnalytics(tracker),
            lifecycleOwner,
            CoroutineScope(testDispatcher),
        )
    }

    @Test
    fun `report stale downloads event`() = runTest(testDispatcher) {
        reporter.setup()

        lifecycleOwner.handleLifecycleEvent(ON_RESUME)
        val (event, properties) = tracker.trackedEvents.single()

        assertEquals(AnalyticsEvent.EPISODE_DOWNLOAD_STALE, event)
        assertEquals(
            mapOf(
                "failed_download_count" to 10L,
                "newest_failed_download" to "1970-01-01T00:00:15Z",
                "oldest_failed_download" to "1970-01-01T00:00:00Z",
            ),
            properties,
        )
    }

    @Test
    fun `report stale downloads event only on resume`() = runTest(testDispatcher) {
        reporter.setup()

        lifecycleOwner.handleLifecycleEvent(ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(ON_START)
        lifecycleOwner.handleLifecycleEvent(ON_STOP)

        assertEquals(0, tracker.trackedEvents.size)

        lifecycleOwner.handleLifecycleEvent(ON_RESUME)

        assertEquals(1, tracker.trackedEvents.size)
    }

    @Test
    fun `report stale downloads event only once`() = runTest(testDispatcher) {
        reporter.setup()

        lifecycleOwner.handleLifecycleEvent(ON_RESUME)
        lifecycleOwner.handleLifecycleEvent(ON_PAUSE)
        lifecycleOwner.handleLifecycleEvent(ON_RESUME)

        assertEquals(1, tracker.trackedEvents.size)
    }

    @Test
    fun `unregister observer after reporting`() = runTest(testDispatcher) {
        reporter.setup()

        lifecycleOwner.handleLifecycleEvent(ON_RESUME)

        assertEquals(0, lifecycleOwner.observerCount)
    }

    @Test
    fun `setup only once`() = runTest(testDispatcher) {
        reporter.setup()
        reporter.setup()

        assertEquals(1, lifecycleOwner.observerCount)

        lifecycleOwner.handleLifecycleEvent(ON_RESUME)
        lifecycleOwner.handleLifecycleEvent(ON_PAUSE)
        reporter.setup()
        lifecycleOwner.handleLifecycleEvent(ON_RESUME)

        assertEquals(0, lifecycleOwner.observerCount)
        assertEquals(1, tracker.trackedEvents.size)
    }

    private class TestTracker : AnalyticsTrackerWrapper() {
        private val _trackedEvents = mutableListOf<Pair<AnalyticsEvent, Map<String, Any>>>()
        val trackedEvents get() = _trackedEvents.toList()

        override fun track(event: AnalyticsEvent, properties: Map<String, Any>) {
            _trackedEvents.add(event to properties)
        }
    }
}
