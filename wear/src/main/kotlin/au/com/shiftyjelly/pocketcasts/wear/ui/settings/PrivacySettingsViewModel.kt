package au.com.shiftyjelly.pocketcasts.wear.ui.settings

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.privacy.UserAnalyticsSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val userAnalyticsSettings: UserAnalyticsSettings,
    settings: Settings,
) : ViewModel() {

    data class State(
        val sendAnalytics: Boolean,
        val sendCrashReports: Boolean,
        val linkCrashReportsToUser: Boolean,
    )

    private val _state = MutableStateFlow(
        State(
            sendAnalytics = settings.collectAnalytics.flow.value,
            sendCrashReports = settings.sendCrashReports.flow.value,
            linkCrashReportsToUser = settings.linkCrashReportsToUser.flow.value,
        )
    )
    val state = _state.asStateFlow()

    fun onAnalyticsChanged(enabled: Boolean) {
        userAnalyticsSettings.updateAnalyticsSetting(enabled)
        _state.update { it.copy(sendAnalytics = enabled) }
    }

    fun onCrashReportingChanged(enabled: Boolean) {
        userAnalyticsSettings.updateCrashReportsSetting(enabled)
        _state.update { it.copy(sendCrashReports = enabled) }
    }

    fun onLinkCrashReportsToUserChanged(enabled: Boolean) {
        userAnalyticsSettings.updateLinkAccountSetting(enabled)
        _state.update { it.copy(linkCrashReportsToUser = enabled) }
    }
}
