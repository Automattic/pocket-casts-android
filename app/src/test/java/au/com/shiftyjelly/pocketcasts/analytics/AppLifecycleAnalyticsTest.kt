package au.com.shiftyjelly.pocketcasts.analytics

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AppLifecycleAnalytics.Companion.KEY_PREVIOUS_VERSION_CODE
import au.com.shiftyjelly.pocketcasts.analytics.AppLifecycleAnalytics.Companion.KEY_TIME_IN_APP
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.PackageUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

private const val VERSION_CODE_DEFAULT = 0
private const val VERSION_CODE_AFTER_FIRST_INSTALL = 1
private const val VERSION_CODE_AFTER_SECOND_INSTALL = 2

@RunWith(MockitoJUnitRunner::class)
class AppLifecycleAnalyticsTest {
    @Mock
    @ApplicationContext
    private lateinit var context: Context

    @Mock
    private lateinit var settings: Settings

    @Mock
    private lateinit var packageUtil: PackageUtil

    @Mock
    private lateinit var analyticsTracker: AnalyticsTrackerWrapper
    lateinit var appLifecycleAnalytics: AppLifecycleAnalytics

    @Before
    fun setUp() {
        appLifecycleAnalytics = AppLifecycleAnalytics(
            context,
            settings,
            packageUtil,
            analyticsTracker
        )
    }

    /* APPLICATION INSTALLED */

    @Test
    fun `given no version code in prefs, when app launched, then app installed event fired`() {
        whenever(settings.getMigratedVersionCode()).thenReturn(VERSION_CODE_DEFAULT)

        appLifecycleAnalytics.onApplicationInstalledOrUpgraded()

        verify(analyticsTracker).track(AnalyticsEvent.APPLICATION_INSTALLED)
    }

    @Test
    fun `given version code in prefs, when app launched, then app installed event not fired`() {
        whenever(settings.getMigratedVersionCode()).thenReturn(VERSION_CODE_AFTER_FIRST_INSTALL)

        appLifecycleAnalytics.onApplicationInstalledOrUpgraded()

        verify(analyticsTracker, never()).track(AnalyticsEvent.APPLICATION_INSTALLED)
    }

    /* APPLICATION UPDATED */

    @Test
    fun `given no version code in prefs, when app launched, then app updated event not fired`() {
        whenever(settings.getMigratedVersionCode()).thenReturn(VERSION_CODE_DEFAULT)

        appLifecycleAnalytics.onApplicationInstalledOrUpgraded()

        verify(analyticsTracker, never()).track(eq(AnalyticsEvent.APPLICATION_UPDATED), any())
    }

    @Test
    fun `given current and last version code different, when app launched, then app updated event fired`() {
        whenever(settings.getMigratedVersionCode()).thenReturn(VERSION_CODE_AFTER_FIRST_INSTALL)
        whenever(packageUtil.getVersionCode(anyOrNull())).thenReturn(
            VERSION_CODE_AFTER_SECOND_INSTALL
        )

        appLifecycleAnalytics.onApplicationInstalledOrUpgraded()

        verify(analyticsTracker)
            .track(
                AnalyticsEvent.APPLICATION_UPDATED,
                mapOf(KEY_PREVIOUS_VERSION_CODE to AnalyticsPropValue(VERSION_CODE_AFTER_FIRST_INSTALL))
            )
    }

    @Test
    fun `given current and last version code same, when app launched, then app updated event not fired`() {
        whenever(settings.getMigratedVersionCode()).thenReturn(VERSION_CODE_AFTER_SECOND_INSTALL)
        whenever(packageUtil.getVersionCode(anyOrNull())).thenReturn(
            VERSION_CODE_AFTER_SECOND_INSTALL
        )

        appLifecycleAnalytics.onApplicationInstalledOrUpgraded()

        verify(analyticsTracker, never()).track(eq(AnalyticsEvent.APPLICATION_UPDATED), any())
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
            mapOf(KEY_TIME_IN_APP to AnalyticsPropValue(1))
        )
    }
}
