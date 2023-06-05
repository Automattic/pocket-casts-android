package au.com.shiftyjelly.pocketcasts.settings.privacy

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid
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
        if (enabled) {
            SentryAndroid.init(context) { it.dsn = settings.getSentryDsn() }
        } else {
            SentryAndroid.init(context) { it.dsn = "" }
        }
        settings.setSendCrashReports(enabled)
    }

    fun updateLinkAccountSetting(enabled: Boolean) {
        val user = if (enabled) User().apply { email = syncManager.getEmail() } else null
        Sentry.setUser(user)

        settings.setLinkCrashReportsToUser(enabled)
    }
}
