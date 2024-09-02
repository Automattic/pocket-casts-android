package au.com.shiftyjelly.pocketcasts.engage

import android.content.Context
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EngageSdkBridge @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val isEngageIntegrationEnabled = AtomicBoolean(false)

    fun registerIntegration() {
        if (FeatureFlag.isEnabled(Feature.ENGAGE_SDK) && !isEngageIntegrationEnabled.getAndSet(true)) {
            EngageSdkWorkers.enqueuePeriodicWork(context)
            EngageSdkLifecycleObserver.register(context)
            EngageSdkBroadcastReceiver.register(context)
        }
    }

    internal companion object {
        const val TAG = "EngageSdk"
    }
}
