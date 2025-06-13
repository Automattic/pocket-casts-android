package au.com.shiftyjelly.pocketcasts.analytics

import android.content.Context
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import java.util.concurrent.atomic.AtomicBoolean
import timber.log.Timber

open class AppsFlyerAnalyticsWrapper {
    private val started = AtomicBoolean(false)

    open fun logEvent(name: String, params: Map<String, Any>, userId: String) = Unit

    open fun startAppsFlyer(userId: String) {
        started.set(true)
    }

    companion object {
        fun setupAppsFlyerLib(context: Context) = Unit
    }
}
