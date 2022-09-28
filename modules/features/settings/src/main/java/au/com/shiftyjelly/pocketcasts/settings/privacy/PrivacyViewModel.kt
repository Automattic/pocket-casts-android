package au.com.shiftyjelly.pocketcasts.settings.privacy

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    val settings: Settings
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        object Error : UiState()
        data class Loaded(
            val analytics: Boolean,
            val crashReports: Boolean,
            val linkAccount: Boolean
        ) : UiState()
    }

    private val mutableUiState = MutableStateFlow<UiState>(UiState.Loaded(analytics = true, crashReports = false, linkAccount = false))
    val uiState: StateFlow<UiState> = mutableUiState.asStateFlow()

    init {
        // settings.privacyAnalyticsFlow
    }

    fun updateAnalyticsSetting(on: Boolean) {
        Timber.i("on: $on")
    }

    fun updateCrashReportsSetting(on: Boolean) {
        Timber.i("on: $on")
    }

    fun updateLinkAccountSetting(on: Boolean) {
        Timber.i("on: $on")
    }
}
