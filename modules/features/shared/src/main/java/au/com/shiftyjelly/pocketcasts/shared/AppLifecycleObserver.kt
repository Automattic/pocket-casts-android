package au.com.shiftyjelly.pocketcasts.shared

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import au.com.shiftyjelly.pocketcasts.analytics.AppLifecycleAnalytics
import au.com.shiftyjelly.pocketcasts.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.featureflag.providers.DefaultReleaseFeatureProvider
import au.com.shiftyjelly.pocketcasts.featureflag.providers.FirebaseRemoteFeatureFlagProvider
import au.com.shiftyjelly.pocketcasts.featureflag.providers.PreferencesFeatureProvider
import javax.inject.Inject

class AppLifecycleObserver @Inject constructor(
    private val appLifecycleAnalytics: AppLifecycleAnalytics,
    private val preferencesFeatureProvider: PreferencesFeatureProvider,
    private val defaultReleaseFeatureProvider: DefaultReleaseFeatureProvider,
    private val firebaseRemoteFeatureProvider: FirebaseRemoteFeatureFlagProvider
) : DefaultLifecycleObserver {
    fun setup() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appLifecycleAnalytics.onApplicationInstalledOrUpgraded()
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

    private fun setupFeatureFlags() {
        val providers = if (BuildConfig.DEBUG) {
            listOf(preferencesFeatureProvider)
        } else {
            listOf(
                firebaseRemoteFeatureProvider,
                storeFeatureProvider
            )
        }
        FeatureFlag.initialize(listOf(providers))
    }
}
