package au.com.shiftyjelly.pocketcasts.utils.featureflag

import au.com.shiftyjelly.pocketcasts.helper.BuildConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import timber.log.Timber

@Singleton
class FirebaseRemoteFeatureInitializer @Inject constructor(
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
) {

    private companion object {
        const val DEFAULT_TIMEOUT = 3_000L
    }

    private val isInitialized = AtomicBoolean(BuildConfig.DEBUG || BuildConfig.IS_PROTOTYPE)

    suspend fun initialize() {
        if (isInitialized.get()) return

        val result = try {
            withTimeout(DEFAULT_TIMEOUT) {
                val success = firebaseRemoteConfig.fetchAndActivate().await()
                Timber.i("Firebase feature flag refreshed")
                success
            }
        } catch (t: Throwable) {
            Timber.e(t, "Failed to refresh Firebase feature flags")
            false
        }
        isInitialized.set(result)
    }
}
