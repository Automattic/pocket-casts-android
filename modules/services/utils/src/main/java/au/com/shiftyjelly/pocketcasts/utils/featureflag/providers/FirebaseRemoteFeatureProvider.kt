package au.com.shiftyjelly.pocketcasts.utils.featureflag.providers

import au.com.shiftyjelly.pocketcasts.utils.config.FirebaseConfig
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.MAX_PRIORITY
import au.com.shiftyjelly.pocketcasts.utils.featureflag.RemoteFeatureProvider
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import javax.inject.Inject
import timber.log.Timber

class FirebaseRemoteFeatureProvider @Inject constructor(
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
) : RemoteFeatureProvider {
    override val priority: Int = MAX_PRIORITY

    override fun isEnabled(feature: Feature) = when (feature) {
        Feature.REPORT_VIOLATION ->
            firebaseRemoteConfig
                .getString(FirebaseConfig.REPORT_VIOLATION_URL)
                .isNotEmpty()

        Feature.SLUMBER_STUDIOS_YEARLY_PROMO ->
            firebaseRemoteConfig
                .getString(FirebaseConfig.SLUMBER_STUDIOS_YEARLY_PROMO_CODE)
                .isNotEmpty()
        else -> firebaseRemoteConfig.getBoolean(feature.key)
    }

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
