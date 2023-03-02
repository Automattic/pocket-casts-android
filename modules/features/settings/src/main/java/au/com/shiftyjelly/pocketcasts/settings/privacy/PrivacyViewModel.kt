package au.com.shiftyjelly.pocketcasts.settings.privacy

import android.content.Context
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.account.SyncAccountManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid
import io.sentry.protocol.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    private val settings: Settings,
    private val syncAccountManager: SyncAccountManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ViewModel() {

    var isFragmentChangingConfigurations: Boolean = false
    sealed class UiState {
        data class Loaded(
            val analytics: Boolean,
            val crashReports: Boolean,
            val linkAccount: Boolean,
            private val getUserEmail: () -> String?
        ) : UiState() {
            fun shouldShowLinkUserSetting() = crashReports && getUserEmail() != null
        }
    }

    private val mutableUiState = MutableStateFlow<UiState>(
        UiState.Loaded(
            analytics = analyticsTracker.getSendUsageStats(),
            crashReports = settings.getSendCrashReports(),
            linkAccount = settings.getLinkCrashReportsToUser(),
            getUserEmail = { getUserEmail() }
        )
    )
    val uiState: StateFlow<UiState> = mutableUiState.asStateFlow()

    fun updateAnalyticsSetting(on: Boolean) {
        if (on) {
            analyticsTracker.setSendUsageStats(true)
            analyticsTracker.track(AnalyticsEvent.ANALYTICS_OPT_IN)
        } else {
            analyticsTracker.track(AnalyticsEvent.ANALYTICS_OPT_OUT)
            analyticsTracker.setSendUsageStats(false)
        }
        mutableUiState.value = (mutableUiState.value as UiState.Loaded).copy(analytics = on)
    }

    fun updateCrashReportsSetting(context: Context, on: Boolean) {
        if (on) {
            SentryAndroid.init(context) { it.dsn = settings.getSentryDsn() }
        } else {
            SentryAndroid.init(context) { it.dsn = "" }
        }
        settings.setSendCrashReports(on)
        mutableUiState.value = (mutableUiState.value as UiState.Loaded).copy(crashReports = on)
    }

    fun updateLinkAccountSetting(on: Boolean) {
        val user = if (on) User().apply { email = getUserEmail() } else null
        Sentry.setUser(user)

        settings.setLinkCrashReportsToUser(on)
        mutableUiState.value = (mutableUiState.value as UiState.Loaded).copy(linkAccount = on)
    }

    private fun getUserEmail() = syncAccountManager.getEmail()
}
