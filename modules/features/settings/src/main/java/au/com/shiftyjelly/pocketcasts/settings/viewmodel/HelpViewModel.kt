package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.repositories.support.DatabaseExportHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class HelpViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val databaseExportHelper: DatabaseExportHelper,
) : ViewModel() {

    private var isFragmentChangingConfigurations = false

    fun onShown() {
        if (!isFragmentChangingConfigurations) {
            analyticsTracker.track(AnalyticsEvent.SETTINGS_HELP_SHOWN)
        }
    }

    fun onFragmentPause(isChangingConfigurations: Boolean?) {
        isFragmentChangingConfigurations = isChangingConfigurations ?: false
    }

    fun onExportDatabaseMenuItemClick(sendIntent: (File) -> Unit) {
        viewModelScope.launch {
            val exportFile = databaseExportHelper.getExportFile()
            exportFile?.let { sendIntent(it) }
        }
    }
}
