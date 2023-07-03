package au.com.shiftyjelly.pocketcasts.analytics

import au.com.shiftyjelly.pocketcasts.utils.timeIntervalSinceNow
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AppLifecycleAnalytics @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
) {
    /* The date the app was last opened, used for calculating time in app */
    private var applicationOpenedDate: Date? = null

    // Called when Pocket Casts is installed on a device that does not already have a previous version of
    // the app installed
    fun onNewApplicationInstall() {
        analyticsTracker.track(AnalyticsEvent.APPLICATION_INSTALLED)
    }

    // Called when Pocket Casts is upgraded on a device
    fun onApplicationUpgrade(previousVersionCode: Int) {
        analyticsTracker.track(
            AnalyticsEvent.APPLICATION_UPDATED,
            mapOf(KEY_PREVIOUS_VERSION_CODE to previousVersionCode)
        )
    }

    fun onApplicationEnterForeground() {
        applicationOpenedDate = Date()
        analyticsTracker.track(AnalyticsEvent.APPLICATION_OPENED)
    }

    fun onApplicationEnterBackground() {
        val properties: MutableMap<String, Any> = HashMap()
        applicationOpenedDate?.let {
            properties[KEY_TIME_IN_APP] = TimeUnit.MILLISECONDS.toSeconds(it.timeIntervalSinceNow()).toInt()
            applicationOpenedDate = null
        }
        analyticsTracker.track(AnalyticsEvent.APPLICATION_CLOSED, properties)
    }

    companion object {
        const val KEY_PREVIOUS_VERSION_CODE = "previous_version_code"
        const val KEY_TIME_IN_APP = "time_in_app" // time in seconds
    }
}
