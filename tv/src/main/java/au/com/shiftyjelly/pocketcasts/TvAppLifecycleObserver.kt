package au.com.shiftyjelly.pocketcasts

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import au.com.shiftyjelly.pocketcasts.analytics.AppLifecycleAnalytics
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.getVersionCode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class TvAppLifecycleObserver @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val appLifecycleAnalytics: AppLifecycleAnalytics,
    private val settings: Settings,
    private val appLifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get(),
) : DefaultLifecycleObserver {

    fun setup() {
        appLifecycleOwner.lifecycle.addObserver(this)
        handleNewInstallOrUpgrade()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        appLifecycleAnalytics.onApplicationEnterForeground()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        appLifecycleAnalytics.onApplicationEnterBackground()
    }

    private fun handleNewInstallOrUpgrade() {
        val versionCode = appContext.getVersionCode()
        val previousVersionCode = settings.getMigratedVersionCode()
        if (previousVersionCode == 0) {
            appLifecycleAnalytics.onNewApplicationInstall()
        } else if (previousVersionCode < versionCode) {
            appLifecycleAnalytics.onApplicationUpgrade(previousVersionCode)
        }
        settings.setMigratedVersionCode(versionCode)
    }
}
