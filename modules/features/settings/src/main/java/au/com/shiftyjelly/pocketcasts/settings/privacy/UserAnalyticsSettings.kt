package au.com.shiftyjelly.pocketcasts.settings.privacy

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import com.automattic.eventhorizon.AnalyticsOptInEvent
import com.automattic.eventhorizon.AnalyticsOptOutEvent
import com.automattic.eventhorizon.CrashReportsToggledEvent
import com.automattic.eventhorizon.EventHorizon
import javax.inject.Inject

class UserAnalyticsSettings @Inject constructor(
    private val settings: Settings,
    private val eventHorizon: EventHorizon,
    private val analyticsTracker: AnalyticsTracker,
) {

    fun updateAnalyticsSetting(enabled: Boolean) {
        if (enabled) {
            settings.collectAnalytics.set(true, updateModifiedAt = true)
            eventHorizon.track(AnalyticsOptInEvent)
        } else {
            eventHorizon.track(AnalyticsOptOutEvent)
            settings.collectAnalytics.set(false, updateModifiedAt = false)
            analyticsTracker.clearAllData()
        }
    }

    fun updateCrashReportsSetting(enabled: Boolean) {
        eventHorizon.track(
            CrashReportsToggledEvent(
                enabled = enabled,
            ),
        )
        settings.sendCrashReports.set(enabled, updateModifiedAt = true)
    }

    fun updateLinkAccountSetting(enabled: Boolean) {
        settings.linkCrashReportsToUser.set(enabled, updateModifiedAt = true)
    }
}
