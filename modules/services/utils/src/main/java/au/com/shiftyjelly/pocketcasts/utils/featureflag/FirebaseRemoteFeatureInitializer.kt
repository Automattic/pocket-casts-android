package au.com.shiftyjelly.pocketcasts.utils.featureflag

import au.com.shiftyjelly.pocketcasts.helper.BuildConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    private val mutex = Mutex()
    private var isInitialized = BuildConfig.DEBUG || BuildConfig.IS_PROTOTYPE

    suspend fun initialize() {
        mutex.withLock {
            if (isInitialized) return

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
            isInitialized = result
        }
    }
}
