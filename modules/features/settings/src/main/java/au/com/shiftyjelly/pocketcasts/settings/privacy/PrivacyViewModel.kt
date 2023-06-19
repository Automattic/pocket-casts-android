package au.com.shiftyjelly.pocketcasts.settings.privacy

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    settings: Settings,
    private val syncManager: SyncManager,
    analyticsTracker: AnalyticsTrackerWrapper,
    private val userAnalyticsSettings: UserAnalyticsSettings,
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
            getUserEmail = { syncManager.getEmail() }
        )
    )
    val uiState: StateFlow<UiState> = mutableUiState.asStateFlow()

    fun updateAnalyticsSetting(on: Boolean) {
        userAnalyticsSettings.updateAnalyticsSetting(on)
        mutableUiState.value = (mutableUiState.value as UiState.Loaded).copy(analytics = on)
    }

    fun updateCrashReportsSetting(on: Boolean) {
        userAnalyticsSettings.updateCrashReportsSetting(on)
        mutableUiState.value = (mutableUiState.value as UiState.Loaded).copy(crashReports = on)
    }

    fun updateLinkAccountSetting(on: Boolean) {
        userAnalyticsSettings.updateLinkAccountSetting(on)
        mutableUiState.value = (mutableUiState.value as UiState.Loaded).copy(linkAccount = on)
    }
}
