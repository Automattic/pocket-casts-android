package au.com.shiftyjelly.pocketcasts.featureflag.providers

import au.com.shiftyjelly.pocketcasts.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.featureflag.MAX_PRIORITY
import au.com.shiftyjelly.pocketcasts.featureflag.RemoteFeatureFlagProvider
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import timber.log.Timber
import javax.inject.Inject

class FirebaseRemoteFeatureFlagProvider @Inject constructor(
    private val firebaseRemoteConfig: FirebaseRemoteConfig
) : RemoteFeatureFlagProvider {
    override val priority: Int = MAX_PRIORITY

    override fun isFeatureEnabled(feature: Feature): Boolean =
        firebaseRemoteConfig.getBoolean(feature.key)

    override fun hasFeature(feature: Feature): Boolean {
        return false
    }

    override fun refreshFeatureFlags() {
        firebaseRemoteConfig.fetch().addOnCompleteListener {
            Timber.e("Firebase feature flag refreshed")
        }
    }
}
