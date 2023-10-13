package au.com.shiftyjelly.pocketcasts.utils.featureflag.providers

import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.MAX_PRIORITY
import au.com.shiftyjelly.pocketcasts.utils.featureflag.RemoteFeatureProvider
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import timber.log.Timber
import javax.inject.Inject

class FirebaseRemoteFeatureProvider @Inject constructor(
    private val firebaseRemoteConfig: FirebaseRemoteConfig
) : RemoteFeatureProvider {
    override val priority: Int = MAX_PRIORITY

    override fun isEnabled(feature: Feature): Boolean =
        firebaseRemoteConfig.getBoolean(feature.key)

    override fun hasFeature(feature: Feature) =
        feature.hasFirebaseRemoteFlag

    override fun refresh() {
        firebaseRemoteConfig.fetch().addOnCompleteListener {
            if (it.isSuccessful) {
                firebaseRemoteConfig.activate()
                Timber.i("Firebase feature flag refreshed")
            } else {
                Timber.e("Could not fetch remote config: ${it.exception?.message ?: "Unknown error"}")
            }
        }
    }
}
