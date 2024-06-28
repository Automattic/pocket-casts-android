package au.com.shiftyjelly.pocketcasts.settings.privacy

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import javax.inject.Inject

class UserAnalyticsSettings @Inject constructor(
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTracker,
) {

    fun updateAnalyticsSetting(enabled: Boolean) {
        if (enabled) {
            settings.collectAnalytics.set(true, updateModifiedAt = true)
            analyticsTracker.track(AnalyticsEvent.ANALYTICS_OPT_IN)
        } else {
            analyticsTracker.track(AnalyticsEvent.ANALYTICS_OPT_OUT)
            settings.collectAnalytics.set(false, updateModifiedAt = false)
            analyticsTracker.clearAllData()
        }
    }

    fun updateCrashReportsSetting(enabled: Boolean) {
        settings.sendCrashReports.set(enabled, updateModifiedAt = true)
    }

    fun updateLinkAccountSetting(enabled: Boolean) {
        settings.linkCrashReportsToUser.set(enabled, updateModifiedAt = true)
    }
}
