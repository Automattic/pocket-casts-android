package au.com.shiftyjelly.pocketcasts.settings.privacy

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ViewModel() {

    var isFragmentChangingConfigurations: Boolean = false
    sealed class UiState {
        data class Loaded(
            val analytics: Boolean,
            val crashReports: Boolean,
            val linkAccount: Boolean
        ) : UiState()
    }

    private val mutableUiState = MutableStateFlow<UiState>(UiState.Loaded(analytics = settings.getSendUsageStats(), crashReports = false, linkAccount = false))
    val uiState: StateFlow<UiState> = mutableUiState.asStateFlow()

    fun updateAnalyticsSetting(on: Boolean) {
        Timber.i("on: $on")
        if (on) {
            settings.setSendUsageStats(true)
            analyticsTracker.track(AnalyticsEvent.ANALYTICS_OPT_IN)
        } else {
            analyticsTracker.track(AnalyticsEvent.ANALYTICS_OPT_OUT)
            settings.setSendUsageStats(false)
        }
        mutableUiState.value = (mutableUiState.value as UiState.Loaded).copy(analytics = on)
    }

    fun updateCrashReportsSetting(on: Boolean) {
        Timber.i("on: $on")
    }

    fun updateLinkAccountSetting(on: Boolean) {
        Timber.i("on: $on")
    }
}
