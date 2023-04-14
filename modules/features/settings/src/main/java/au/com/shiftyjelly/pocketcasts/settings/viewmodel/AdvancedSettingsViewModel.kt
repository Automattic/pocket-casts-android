package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.FileUtilWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.*
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class AdvancedSettingsViewModel
@Inject constructor(
    private val podcastManager: PodcastManager,
    private val episodeManager: EpisodeManager,
    private val fileStorage: FileStorage,
    private val fileUtil: FileUtilWrapper,
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val mutableState = MutableStateFlow(initState())
    val state: StateFlow<State> = mutableState

    private val mutableSnackbarMessage = MutableSharedFlow<Int>()
    val snackbarMessage = mutableSnackbarMessage.asSharedFlow()

    private val backgroundRefreshSummary: Int
        get() = if (settings.refreshPodcastsAutomatically()) {
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
//        setupStorage()
    }

//
//    private fun onStorageDataWarningCheckedChange(isChecked: Boolean) {
//        settings.setWarnOnMeteredNetwork(isChecked)
//        updateMobileDataWarningState()
//    }
//
//    private fun updateMobileDataWarningState() {
//        mutableState.value = mutableState.value.copy(
//            storageDataWarningState = mutableState.value.storageDataWarningState.copy(
//                isChecked = settings.warnOnMeteredNetwork(),
//            )
//        )
//    }

    private fun onSyncOnMeteredCheckedChange(isChecked: Boolean) {
        settings.setSyncOnMeteredNetwork(isChecked)
        updateSyncOnMeteredState()
    }

    private fun updateSyncOnMeteredState() {
        mutableState.value = mutableState.value.copy(
            backgroundSyncOnMeteredState = mutableState.value.backgroundSyncOnMeteredState.copy(
                isChecked = settings.syncOnMeteredNetwork(),
                summary = backgroundRefreshSummary
            )
        )
    }

//    private fun createAlertDialogState(
//        title: String,
//        @StringRes message: Int? = null,
//    ) = AlertDialogState(
//        title = title,
//        message = message?.let { context.getString(message) },
//        buttons = listOf(
//            DialogButtonState(
//                text = context.getString(LR.string.cancel).uppercase(
//                    Locale.getDefault()
//                ),
//                onClick = {}
//            ),
//            DialogButtonState(
//                text = context.getString(LR.string.ok),
//                onClick = {}
//            )
//        )
//    )

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

//    data class AlertDialogState(
//        val title: String,
//        val message: String? = null,
//        val buttons: List<DialogButtonState>,
//    )
}
