package au.com.shiftyjelly.pocketcasts.analytics

import android.content.Context
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.appsflyer.AppsFlyerLib
import com.appsflyer.attribution.AppsFlyerRequestListener
import java.util.concurrent.atomic.AtomicBoolean
import timber.log.Timber

open class AppsFlyerAnalyticsWrapper(
    private val appsFlyerLib: AppsFlyerLib?,
    private val context: Context,
) {
    private val started = AtomicBoolean(false)
    private val logEventListener = object : AppsFlyerRequestListener {
        override fun onSuccess() {
            Timber.i("AppsFlyer event logged successfully")
        }

        override fun onError(code: Int, description: String) {
            LogBuffer.e("AppsFlyer", "AppsFlyer event logging failed. Error code: $code, description: $description")
        }
    }
    private val launchEventListener = object : AppsFlyerRequestListener {
        override fun onSuccess() {
            Timber.i("AppsFlyer launch successful")
        }

        override fun onError(code: Int, description: String) {
            LogBuffer.e("AppsFlyer", "AppsFlyer launch failed. Error code: $code, description: $description")
        }
    }

    open fun logEvent(name: String, params: Map<String, Any>, userId: String) {
        if (appsFlyerLib == null) {
            return
        }
        if (!started.get()) {
            startAppsFlyer(userId)
        }
        appsFlyerLib.logEvent(context, name, params, logEventListener)
    }

    open fun startAppsFlyer(userId: String) {
        if (appsFlyerLib == null) {
            return
        }
        appsFlyerLib.start(context, BuildConfig.APPS_FLYER_KEY, launchEventListener)
        appsFlyerLib.setCustomerIdAndLogSession(userId, context)
        started.set(true)
    }

    companion object {
        fun setupAppsFlyerLib(context: Context): AppsFlyerLib? {
            return try {
                AppsFlyerLib.getInstance().apply {
                    init(BuildConfig.APPS_FLYER_KEY, null, context)
                    setDebugLog(BuildConfig.DEBUG)
                    waitForCustomerUserId(true)
                }
            } catch (throwable: Throwable) {
                LogBuffer.e("AppsFlyer", throwable, "Failed initializing AppsFlyer")
                null
            }
        }
    }
}
