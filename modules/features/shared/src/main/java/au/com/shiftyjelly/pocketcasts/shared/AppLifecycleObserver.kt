package au.com.shiftyjelly.pocketcasts.shared

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import au.com.shiftyjelly.pocketcasts.analytics.AppLifecycleAnalytics
import au.com.shiftyjelly.pocketcasts.featureflag.FeatureFlagManager
import au.com.shiftyjelly.pocketcasts.featureflag.providers.FirebaseRemoteFeatureFlagProvider
import au.com.shiftyjelly.pocketcasts.featureflag.providers.PreferencesFeatureFlagProvider
import au.com.shiftyjelly.pocketcasts.featureflag.providers.StoreFeatureFlagProvider
import javax.inject.Inject

class AppLifecycleObserver @Inject constructor(
    private val appLifecycleAnalytics: AppLifecycleAnalytics,
    private val preferencesFeatureFlagProvider: PreferencesFeatureFlagProvider,
    private val storeFeatureFlagProvider: StoreFeatureFlagProvider,
    private val firebaseRemoteFeatureFlagProvider: FirebaseRemoteFeatureFlagProvider
) : DefaultLifecycleObserver {
    fun setup() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appLifecycleAnalytics.onApplicationInstalledOrUpgraded()
        setupFeatureFlags()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        appLifecycleAnalytics.onApplicationEnterForeground()
        FeatureFlagManager.refreshFeatureFlags()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        appLifecycleAnalytics.onApplicationEnterBackground()
    }

    private fun setupFeatureFlags() {
        val providers = if (BuildConfig.DEBUG) {
            listOf(preferencesFeatureFlagProvider)
        } else {
            listOf(
                firebaseRemoteFeatureFlagProvider,
                storeFeatureFlagProvider
            )
        }
        FeatureFlagManager.initialize(providers)
    }
}
