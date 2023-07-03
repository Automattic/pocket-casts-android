package au.com.shiftyjelly.pocketcasts.analytics

import au.com.shiftyjelly.pocketcasts.analytics.AppLifecycleAnalytics.Companion.KEY_PREVIOUS_VERSION_CODE
import au.com.shiftyjelly.pocketcasts.analytics.AppLifecycleAnalytics.Companion.KEY_TIME_IN_APP
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify

@RunWith(MockitoJUnitRunner::class)
class AppLifecycleAnalyticsTest {
    @Mock
    private lateinit var analyticsTracker: AnalyticsTrackerWrapper
    lateinit var appLifecycleAnalytics: AppLifecycleAnalytics

    @Before
    fun setUp() {
        appLifecycleAnalytics = AppLifecycleAnalytics(analyticsTracker)
    }

    /* APPLICATION INSTALLED */

    @Test
    fun `when app is installed, then installed event fired`() {
        appLifecycleAnalytics.onNewApplicationInstall()

        verify(analyticsTracker).track(AnalyticsEvent.APPLICATION_INSTALLED)
    }

    /* APPLICATION UPDATED */

    @Test
    fun `when app is updated, then updated event fired`() {

        val previousVersionCode = 123
        appLifecycleAnalytics.onApplicationUpgrade(previousVersionCode)

        verify(analyticsTracker).track(
            AnalyticsEvent.APPLICATION_UPDATED,
            mapOf(KEY_PREVIOUS_VERSION_CODE to previousVersionCode)
        )
    }

    /* APPLICATION OPENED */

    @Test
    fun `when app is foregrounded, then app opened event fired`() {
        appLifecycleAnalytics.onApplicationEnterForeground()

        verify(analyticsTracker).track(AnalyticsEvent.APPLICATION_OPENED)
    }

    /* APPLICATION CLOSED */

    @Test
    fun `when app is backgrounded, then app closed event fired with time in app`() {
        appLifecycleAnalytics.onApplicationEnterForeground()
        Thread.sleep(1000)
        appLifecycleAnalytics.onApplicationEnterBackground()

        verify(analyticsTracker).track(
            AnalyticsEvent.APPLICATION_CLOSED,
            mapOf(KEY_TIME_IN_APP to 1)
        )
    }
}
