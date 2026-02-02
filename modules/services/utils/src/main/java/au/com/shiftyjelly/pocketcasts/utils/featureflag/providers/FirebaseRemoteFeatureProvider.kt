package au.com.shiftyjelly.pocketcasts.utils.featureflag.providers

import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
class FirebaseRemoteFeatureProvider @Inject constructor(
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
    @ApplicationScope private val scope: CoroutineScope,
) : FeatureProvider {
    private val initialFetchComplete = CompletableDeferred<Boolean>()

    override val priority: Int = MAX_PRIORITY

    init {
        scope.launch {
            firebaseRemoteConfig.fetchSuspending()
                .map { firebaseRemoteConfig.activateSuspending().getOrThrow() }
                .onSuccess {
                    Timber.i("Firebase feature flag refreshed")
                    initialFetchComplete.complete(true)
                }
                .onFailure {
                    Timber.e(it, "Failed to refresh Firebase feature flags")
                    initialFetchComplete.complete(false)
                }

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

    override suspend fun awaitInitialization() = initialFetchComplete.await()
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
