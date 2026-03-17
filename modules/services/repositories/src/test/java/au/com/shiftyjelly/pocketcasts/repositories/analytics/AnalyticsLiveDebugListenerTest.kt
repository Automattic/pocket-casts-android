package au.com.shiftyjelly.pocketcasts.repositories.analytics

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.appreview.TestSetting
import au.com.shiftyjelly.pocketcasts.servers.analytics.AnalyticsLiveServiceManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MutableClock
import com.automattic.eventhorizon.PodcastListSortType
import com.automattic.eventhorizon.PodcastsListFolderTappedEvent
import com.automattic.eventhorizon.PodcastsListSortOrderChangedEvent
import com.automattic.eventhorizon.ProfileTabOpenedEvent
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AnalyticsLiveDebugListenerTest {

    private val liveAnalyticsUrlFlow = MutableStateFlow("")
    private val liveAnalyticsUrlSetting = mock<UserSetting<String>> {
        on { flow } doReturn liveAnalyticsUrlFlow
    }
    private val collectAnalyticsSetting = TestSetting(true)
    private val clock = MutableClock(Instant.parse("2026-01-01T00:00:00Z"))

    private val serviceManager = mock<AnalyticsLiveServiceManager>()
    private val settings = mock<Settings> {
        on { liveAnalyticsUrl } doReturn liveAnalyticsUrlSetting
        on { collectAnalytics } doReturn collectAnalyticsSetting
    }

    @Test
    fun `does not send events when live analytics URL is blank`() = runTest(UnconfinedTestDispatcher()) {
        val listener = createListener(backgroundScope)

        listener.onEvent(ProfileTabOpenedEvent(initial = true))

        verify(serviceManager, never()).sendEvents(any(), any())
    }

    @Test
    fun `does not send events when analytics collection is disabled`() = runTest(UnconfinedTestDispatcher()) {
        collectAnalyticsSetting.set(false)
        liveAnalyticsUrlFlow.value = "https://live.pocketcasts.com/events"

        val listener = createListener(backgroundScope)

        listener.onEvent(ProfileTabOpenedEvent(initial = true))

        verify(serviceManager, never()).sendEvents(any(), any())
    }

    @Test
    fun `sends events when URL is set and analytics are enabled`() = runTest(UnconfinedTestDispatcher()) {
        liveAnalyticsUrlFlow.value = "https://live.pocketcasts.com/events"

        val listener = createListener(backgroundScope)
        listener.onEvent(PodcastsListFolderTappedEvent)

        verify(serviceManager).sendEvents(
            eq("https://live.pocketcasts.com/events"),
            argThat { size == 1 && first().name == PodcastsListFolderTappedEvent.analyticsName },
        )
    }

    @Test
    fun `sends event with correct properties`() = runTest(UnconfinedTestDispatcher()) {
        liveAnalyticsUrlFlow.value = "https://live.pocketcasts.com/events"

        val listener = createListener(backgroundScope)
        listener.onEvent(PodcastsListSortOrderChangedEvent(sortBy = PodcastListSortType.Name))

        verify(serviceManager).sendEvents(
            any(),
            argThat {
                val event = first()
                event.name == PodcastsListSortOrderChangedEvent.EventName &&
                    event.platform == "Android" &&
                    event.timestamp == clock.instant() &&
                    event.properties.value == mapOf("sort_by" to PodcastListSortType.Name.toString())
            },
        )
    }

    @Test
    fun `batches multiple events together`() = runTest(UnconfinedTestDispatcher()) {
        val listener = createListener(backgroundScope)
        repeat(5) { _ ->
            listener.onEvent(PodcastsListFolderTappedEvent)
        }

        liveAnalyticsUrlFlow.value = "https://live.pocketcasts.com/events"

        verify(serviceManager).sendEvents(
            any(),
            argThat { size == 5 },
        )
    }

    @Test
    fun `stops sending events when URL is cleared`() = runTest(UnconfinedTestDispatcher()) {
        liveAnalyticsUrlFlow.value = "https://live.pocketcasts.com/events"

        val listener = createListener(backgroundScope)

        liveAnalyticsUrlFlow.value = ""

        listener.onEvent(ProfileTabOpenedEvent(initial = true))

        verify(serviceManager, never()).sendEvents(any(), any())
    }

    @Test
    fun `stops sending events when analytics collection is disabled`() = runTest(UnconfinedTestDispatcher()) {
        liveAnalyticsUrlFlow.value = "https://live.pocketcasts.com/events"

        val listener = createListener(backgroundScope)

        collectAnalyticsSetting.set(false)

        listener.onEvent(ProfileTabOpenedEvent(initial = true))

        verify(serviceManager, never()).sendEvents(any(), any())
    }

    @Test
    fun `uses correct timestamp from clock`() = runTest(UnconfinedTestDispatcher()) {
        liveAnalyticsUrlFlow.value = "https://live.pocketcasts.com/events"
        val expectedTimestamp = clock.instant()

        val listener = createListener(backgroundScope)
        listener.onEvent(ProfileTabOpenedEvent(initial = true))

        verify(serviceManager).sendEvents(
            any(),
            argThat { first().timestamp == expectedTimestamp },
        )
    }

    private fun createListener(scope: CoroutineScope) = AnalyticsLiveDebugListener(
        settings = settings,
        analyticsLiveServiceManager = serviceManager,
        clock = clock,
        coroutineScope = scope,
    )
}
