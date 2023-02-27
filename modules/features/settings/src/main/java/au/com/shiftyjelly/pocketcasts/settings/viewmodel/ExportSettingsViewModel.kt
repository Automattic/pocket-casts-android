package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ExportSettingsViewModel @Inject constructor(
    val analyticsTracker: AnalyticsTrackerWrapper,
) : ViewModel() {

    private var isFragmentChangingConfigurations: Boolean = false

    fun onCreate() {
        if (!isFragmentChangingConfigurations) {
            analyticsTracker.track(AnalyticsEvent.SETTINGS_IMPORT_SHOWN)
        }
    }

    fun onFragmentPause(isChangingConfigurations: Boolean?) {
        isFragmentChangingConfigurations = isChangingConfigurations ?: false
    }

    fun onImportSelectFile() {
        analyticsTracker.track(AnalyticsEvent.SETTINGS_IMPORT_SELECT_FILE)
    }

    fun onImportByUrlClicked() {
        analyticsTracker.track(AnalyticsEvent.SETTINGS_IMPORT_BY_URL)
    }

    fun onExportByEmail() {
        analyticsTracker.track(AnalyticsEvent.SETTINGS_IMPORT_EXPORT_EMAIL_TAPPED)
    }

    fun onExportFile() {
        analyticsTracker.track(AnalyticsEvent.SETTINGS_IMPORT_EXPORT_FILE_TAPPED)
    }
}
