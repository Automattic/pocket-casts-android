package au.com.shiftyjelly.pocketcasts.repositories.refresh

import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class RefreshPodcastsTaskSchedulingTest {
    private val context = RuntimeEnvironment.getApplication()
    private val backgroundRefresh = mock<UserSetting<Boolean>> {
        on { value } doReturn true
    }
    private var networkType = NetworkType.CONNECTED
    private val settings = mock<Settings> {
        on { backgroundRefreshPodcasts } doReturn backgroundRefresh
        on { getWorkManagerNetworkTypeConstraint() } doAnswer { networkType }
    }

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @Test
    fun `schedule a single periodic refresh`() {
        RefreshPodcastsTask.scheduleOrCancel(context, settings)

        val work = scheduledWork()
        assertEquals(1, work.size)
        assertEquals(WorkInfo.State.ENQUEUED, work.single().state)
    }

    @Test
    fun `keep the same work when scheduling again`() {
        RefreshPodcastsTask.scheduleOrCancel(context, settings)
        val firstId = scheduledWork().single().id

        // The app does this every time it is opened. A refresh that is already running must not be cancelled.
        RefreshPodcastsTask.scheduleOrCancel(context, settings)

        assertEquals(listOf(firstId), scheduledWork().map { it.id })
    }

    @Test
    fun `apply changed constraints when scheduling again`() {
        RefreshPodcastsTask.scheduleOrCancel(context, settings)

        networkType = NetworkType.UNMETERED
        RefreshPodcastsTask.scheduleOrCancel(context, settings)

        assertEquals(NetworkType.UNMETERED, scheduledWork().single().constraints.requiredNetworkType)
    }

    @Test
    fun `cancel the work when background refresh is turned off`() {
        RefreshPodcastsTask.scheduleOrCancel(context, settings)

        whenever(backgroundRefresh.value).thenReturn(false)
        RefreshPodcastsTask.scheduleOrCancel(context, settings)

        val work = scheduledWork()
        assertEquals(1, work.size)
        assertTrue(work.all { it.state == WorkInfo.State.CANCELLED })
    }

    private fun scheduledWork() = WorkManager.getInstance(context).getWorkInfosByTag(TAG_REFRESH_TASK).get()

    private companion object {
        const val TAG_REFRESH_TASK = "au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsTask"
    }
}
