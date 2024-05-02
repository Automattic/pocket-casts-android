package au.com.shiftyjelly.pocketcasts.settings.privacy

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sentry.Sentry
import io.sentry.protocol.User
import javax.inject.Inject

class UserAnalyticsSettings @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: Settings,
    private val syncManager: SyncManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) {

    fun updateAnalyticsSetting(enabled: Boolean) {
        if (enabled) {
            analyticsTracker.setSendUsageStats(true)
            analyticsTracker.track(AnalyticsEvent.ANALYTICS_OPT_IN)
        } else {
            analyticsTracker.track(AnalyticsEvent.ANALYTICS_OPT_OUT)
            analyticsTracker.setSendUsageStats(false)
        }
    }

    fun updateCrashReportsSetting(enabled: Boolean) {
        settings.sendCrashReports.set(enabled, updateModifiedAt = true)
    }

    fun updateLinkAccountSetting(enabled: Boolean) {
        settings.linkCrashReportsToUser.set(enabled, updateModifiedAt = true)
    }
}
