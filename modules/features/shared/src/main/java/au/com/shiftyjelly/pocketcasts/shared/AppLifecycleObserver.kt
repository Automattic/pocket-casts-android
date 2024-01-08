package au.com.shiftyjelly.pocketcasts.shared

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import au.com.shiftyjelly.pocketcasts.analytics.AppLifecycleAnalytics
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import au.com.shiftyjelly.pocketcasts.utils.PackageUtil
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.featureflag.providers.DefaultReleaseFeatureProvider
import au.com.shiftyjelly.pocketcasts.utils.featureflag.providers.FirebaseRemoteFeatureProvider
import au.com.shiftyjelly.pocketcasts.utils.featureflag.providers.PreferencesFeatureProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

class AppLifecycleObserver constructor(
    @ApplicationContext private val appContext: Context,
    private val appLifecycleAnalytics: AppLifecycleAnalytics,
    private val appLifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get(),
    private val preferencesFeatureProvider: PreferencesFeatureProvider,
    private val defaultReleaseFeatureProvider: DefaultReleaseFeatureProvider,
    private val firebaseRemoteFeatureProvider: FirebaseRemoteFeatureProvider,
    private val packageUtil: PackageUtil,
    private val settings: Settings,
    private val applicationScope: CoroutineScope,
) : DefaultLifecycleObserver {

    @Inject
    constructor(
        @ApplicationContext appContext: Context,
        appLifecycleAnalytics: AppLifecycleAnalytics,
        preferencesFeatureProvider: PreferencesFeatureProvider,
        defaultReleaseFeatureProvider: DefaultReleaseFeatureProvider,
        firebaseRemoteFeatureProvider: FirebaseRemoteFeatureProvider,
        packageUtil: PackageUtil,
        settings: Settings,
        @ApplicationScope applicationScope: CoroutineScope,
    ) : this(
        appContext = appContext,
        appLifecycleAnalytics = appLifecycleAnalytics,
        appLifecycleOwner = ProcessLifecycleOwner.get(),
        preferencesFeatureProvider = preferencesFeatureProvider,
        defaultReleaseFeatureProvider = defaultReleaseFeatureProvider,
        firebaseRemoteFeatureProvider = firebaseRemoteFeatureProvider,
        packageUtil = packageUtil,
        settings = settings,
        applicationScope = applicationScope,
    )

    fun setup() {
        appLifecycleOwner.lifecycle.addObserver(this)
        handleNewInstallOrUpgrade()
        setupFeatureFlags()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        appLifecycleAnalytics.onApplicationEnterForeground()
        FeatureFlag.refresh()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        appLifecycleAnalytics.onApplicationEnterBackground()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        applicationScope.cancel("Application onTerminate")
        super.onDestroy(owner)
    }

    private fun setupFeatureFlags() {
        val providers = if (BuildConfig.DEBUG) {
            listOf(preferencesFeatureProvider)
        } else {
            listOf(
                firebaseRemoteFeatureProvider,
                defaultReleaseFeatureProvider,
            )
        }
        FeatureFlag.initialize(providers)
    }

    private fun handleNewInstallOrUpgrade() {
        // Track app upgrade and install
        val versionCode = packageUtil.getVersionCode(appContext)
        val previousVersionCode = settings.getMigratedVersionCode()

        val isNewInstall = previousVersionCode == 0
        if (isNewInstall) {
            appLifecycleAnalytics.onNewApplicationInstall()

            // new installs default to not forcing up next to use the dark theme
            settings.useDarkUpNextTheme.set(false)

            when (getAppPlatform()) {
                // do nothing because this already defaults to true for all users on automotive
                AppPlatform.Automotive -> {}

                // do nothing because feature has not been enabled on Wear OS yet
                AppPlatform.WearOs -> {}

                // For new users we want to auto play when the queue is empty by default
                AppPlatform.Phone -> settings.autoPlayNextEpisodeOnEmpty.set(true)
            }
        } else if (previousVersionCode < versionCode) {
            appLifecycleAnalytics.onApplicationUpgrade(previousVersionCode)
        }
    }

    @VisibleForTesting
    fun getAppPlatform() = Util.getAppPlatform(appContext)
}
