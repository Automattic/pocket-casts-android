package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsTask
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class AdvancedSettingsViewModel
@Inject constructor(
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val mutableState = MutableStateFlow(initState())
    val state: StateFlow<State> = mutableState

    private val mutableSnackbarMessage = MutableSharedFlow<Int>()
    val snackbarMessage = mutableSnackbarMessage.asSharedFlow()

    private val backgroundRefreshSummary: Int
        get() = if (settings.syncOnMeteredNetwork()) {
            LR.string.settings_advanced_sync_on_metered_on
        } else {
            LR.string.settings_advanced_sync_on_metered_off
        }

    private var sdkVersion: Int = 0

    fun start(
        sdkVersion: Int = Build.VERSION.SDK_INT,
    ) {
        this.sdkVersion = sdkVersion
    }

    private fun initState() = State(
        backgroundSyncOnMeteredState = State.BackgroundSyncOnMeteredState(
            summary = backgroundRefreshSummary,
            isChecked = settings.syncOnMeteredNetwork(),
            onCheckedChange = {
                onSyncOnMeteredCheckedChange(it)
                analyticsTracker.track(
                    AnalyticsEvent.SETTINGS_ADVANCED_SYNC_ON_METERED,
                    mapOf("enabled" to it)
                )
            }
        )
    )

    fun onFragmentResume() {
    }

    fun doPodcastsRefreshAutomatically(): Boolean {
        return settings.refreshPodcastsAutomatically()
    }

    private fun onSyncOnMeteredCheckedChange(isChecked: Boolean) {
        settings.setSyncOnMeteredNetwork(isChecked)
        updateSyncOnMeteredState()

        // Update worker to take sync setting into account
        RefreshPodcastsTask.scheduleOrCancel(context, settings)
    }

    private fun updateSyncOnMeteredState() {
        mutableState.value = mutableState.value.copy(
            backgroundSyncOnMeteredState = mutableState.value.backgroundSyncOnMeteredState.copy(
                isChecked = settings.syncOnMeteredNetwork(),
                summary = backgroundRefreshSummary
            )
        )
    }

    fun onShown() {
        analyticsTracker.track(AnalyticsEvent.SETTINGS_ADVANCED_SHOWN)
    }

    data class State(
        val backgroundSyncOnMeteredState: BackgroundSyncOnMeteredState
    ) {

        data class BackgroundSyncOnMeteredState(
            @StringRes val summary: Int,
            val isChecked: Boolean = true,
            val onCheckedChange: (Boolean) -> Unit,
        )
    }
}
