package au.com.shiftyjelly.pocketcasts.utils.featureflag.providers

import au.com.shiftyjelly.pocketcasts.utils.config.FirebaseConfig
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.featureflag.MAX_PRIORITY
import au.com.shiftyjelly.pocketcasts.utils.featureflag.RemoteFeatureProvider
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.configUpdates
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
class FirebaseRemoteFeatureProvider(
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
    private val scope: CoroutineScope,
) : RemoteFeatureProvider {
    @Inject constructor(firebaseRemoteConfig: FirebaseRemoteConfig) : this(
        firebaseRemoteConfig,
        @Suppress("OPT_IN_USAGE")
        // Using GlobalScope here is perfectly fine. This type is instantiated as a singleton.
        // We have @ApplicationScope but it lives in a different module which cannot be included here.
        // GlobalScope and @ApplicationScope are effectively the same as they have the same lifetime.
        GlobalScope,
    )

    override val priority: Int = MAX_PRIORITY

    init {
        scope.launch {
            firebaseRemoteConfig.configUpdates.collect {
                FeatureFlag.updateFeatureFlowValues()
            }
        }
    }

    override fun isEnabled(feature: Feature) = when (feature) {
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
