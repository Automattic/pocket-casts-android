package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import androidx.lifecycle.ViewModel
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SettingsImportByUrlEvent
import com.automattic.eventhorizon.SettingsImportExportEmailTappedEvent
import com.automattic.eventhorizon.SettingsImportExportFileTappedEvent
import com.automattic.eventhorizon.SettingsImportSelectFileEvent
import com.automattic.eventhorizon.SettingsImportShownEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ExportSettingsViewModel @Inject constructor(
    val eventHorizon: EventHorizon,
) : ViewModel() {

    private var isFragmentChangingConfigurations: Boolean = false

    fun onCreate() {
        if (!isFragmentChangingConfigurations) {
            eventHorizon.track(SettingsImportShownEvent)
        }
    }

    fun onFragmentPause(isChangingConfigurations: Boolean?) {
        isFragmentChangingConfigurations = isChangingConfigurations ?: false
    }

    fun onImportSelectFile() {
        eventHorizon.track(SettingsImportSelectFileEvent)
    }

    fun onImportByUrlClicked() {
        eventHorizon.track(SettingsImportByUrlEvent)
    }

    fun onExportByEmail() {
        eventHorizon.track(SettingsImportExportEmailTappedEvent)
    }

    fun onExportFile() {
        eventHorizon.track(SettingsImportExportFileTappedEvent)
    }
}
