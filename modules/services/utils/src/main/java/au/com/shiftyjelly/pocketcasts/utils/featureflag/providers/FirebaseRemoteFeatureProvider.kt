package au.com.shiftyjelly.pocketcasts.utils.featureflag.providers

import au.com.shiftyjelly.pocketcasts.utils.config.FirebaseConfig
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureProvider
import au.com.shiftyjelly.pocketcasts.utils.featureflag.MAX_PRIORITY
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.configUpdates
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
class FirebaseRemoteFeatureProvider(
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
    private val scope: CoroutineScope,
) : FeatureProvider {
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
            firebaseRemoteConfig.fetchSuspending()
                .map { firebaseRemoteConfig.activateSuspending().getOrThrow() }
                .onSuccess { Timber.i("Firebase feature flag refreshed") }
                .onFailure { Timber.e(it, "Failed to refresh Firebase feature flags") }

            firebaseRemoteConfig.configUpdates.collect {
                firebaseRemoteConfig.activateSuspending()
                    .onSuccess { Timber.i("Firebase feature flag refreshed") }
                    .onFailure { Timber.e(it, "Failed to refresh Firebase feature flags") }
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

    override fun hasFeature(feature: Feature) = feature.hasFirebaseRemoteFlag
}

private suspend fun FirebaseRemoteConfig.fetchSuspending() = suspendCoroutine<Result<Unit>> { continuation ->
    fetch().addOnCompleteListener { task ->
        val exception = task.exception
        continuation.resume(
            if (exception == null) {
                Result.success(Unit)
            } else {
                Result.failure(exception)
            },
        )
    }
}

private suspend fun FirebaseRemoteConfig.activateSuspending() = suspendCoroutine<Result<Unit>> { continuation ->
    activate().addOnCompleteListener { task ->
        val exception = task.exception
        continuation.resume(
            if (exception == null) {
                Result.success(Unit)
            } else {
                Result.failure(exception)
            },
        )
    }
}
