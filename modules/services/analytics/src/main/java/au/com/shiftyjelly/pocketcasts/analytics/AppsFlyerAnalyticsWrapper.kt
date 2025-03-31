package au.com.shiftyjelly.pocketcasts.analytics

import android.content.Context
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.appsflyer.AppsFlyerLib
import com.appsflyer.attribution.AppsFlyerRequestListener
import java.util.concurrent.atomic.AtomicBoolean
import timber.log.Timber

open class AppsFlyerAnalyticsWrapper(
    private val appsFlyerLib: AppsFlyerLib?,
    private val isTrackingEnabled: () -> Boolean,
    private val context: Context,
) {
    private val started = AtomicBoolean(false)
    private val logEventListener = object : AppsFlyerRequestListener {
        override fun onSuccess() {
            Timber.i("AppsFlyer event logged successfully")
        }

        override fun onError(code: Int, descrption: String) {
            LogBuffer.e("AppsFlyer", "AppsFlyer event logging failed. Error code: $code, description: $descrption")
        }
    }
    private val launchEventListener = object : AppsFlyerRequestListener {
        override fun onSuccess() {
            Timber.i("AppsFlyer launch successful")
        }

        override fun onError(code: Int, descrption: String) {
            LogBuffer.e("AppsFlyer", "AppsFlyer launch failed. Error code: $code, description: $descrption")
        }
    }

    open fun logEvent(name: String, params: Map<String, Any>, userId: String) {
        val isDisabled = !FeatureFlag.isEnabled(Feature.APPSFLYER_ANALYTICS)
        if (isDisabled || appsFlyerLib == null || !isTrackingEnabled()) {
            return
        }
        if (!started.get()) {
            startAppsFlyer(appsFlyerLib, context, userId)
        }
        appsFlyerLib.logEvent(context, name, params, logEventListener)
    }

    private fun startAppsFlyer(appsFlyerLib: AppsFlyerLib, context: Context, userId: String) {
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
