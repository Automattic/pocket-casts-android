package au.com.shiftyjelly.pocketcasts

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import au.com.shiftyjelly.pocketcasts.analytics.AppLifecycleAnalytics
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

private const val PACKAGE_NAME = "au.com.shiftyjelly.pocketcasts.tv"
private const val VERSION_CODE_FRESH_INSTALL = 0
private const val VERSION_CODE_PREVIOUS = 1
private const val VERSION_CODE_CURRENT = 2

@RunWith(MockitoJUnitRunner::class)
class TvAppLifecycleObserverTest {

    @Mock private lateinit var context: Context

    @Mock private lateinit var packageManager: PackageManager

    @Mock private lateinit var appLifecycleAnalytics: AppLifecycleAnalytics

    @Mock private lateinit var settings: Settings

    @Mock private lateinit var appLifecycleOwner: LifecycleOwner

    @Mock private lateinit var appLifecycle: Lifecycle

    private lateinit var observer: TvAppLifecycleObserver

    @Suppress("DEPRECATION")
    @Before
    fun setUp() {
        whenever(context.packageName).thenReturn(PACKAGE_NAME)
        whenever(context.packageManager).thenReturn(packageManager)
        whenever(packageManager.getPackageInfo(PACKAGE_NAME, 0)).thenReturn(
            PackageInfo().apply { versionCode = VERSION_CODE_CURRENT },
        )
        whenever(appLifecycleOwner.lifecycle).thenReturn(appLifecycle)

        observer = TvAppLifecycleObserver(
            appContext = context,
            appLifecycleAnalytics = appLifecycleAnalytics,
            settings = settings,
            appLifecycleOwner = appLifecycleOwner,
        )
    }

    @Test
    fun handlesNewInstall() {
        whenever(settings.getMigratedVersionCode()).thenReturn(VERSION_CODE_FRESH_INSTALL)

        observer.setup()

        verify(appLifecycleAnalytics).onNewApplicationInstall()
        verify(appLifecycleAnalytics, never()).onApplicationUpgrade(any())
        verify(settings).setMigratedVersionCode(VERSION_CODE_CURRENT)
    }

    @Test
    fun handlesUpgrade() {
        whenever(settings.getMigratedVersionCode()).thenReturn(VERSION_CODE_PREVIOUS)

        observer.setup()

        verify(appLifecycleAnalytics).onApplicationUpgrade(VERSION_CODE_PREVIOUS)
        verify(appLifecycleAnalytics, never()).onNewApplicationInstall()
        verify(settings).setMigratedVersionCode(VERSION_CODE_CURRENT)
    }

    @Test
    fun sameVersionDoesNotTrackOrRewriteMigratedVersion() {
        whenever(settings.getMigratedVersionCode()).thenReturn(VERSION_CODE_CURRENT)

        observer.setup()

        verify(appLifecycleAnalytics, never()).onNewApplicationInstall()
        verify(appLifecycleAnalytics, never()).onApplicationUpgrade(any())
        verify(settings, never()).setMigratedVersionCode(any())
    }
}
