package au.com.shiftyjelly.pocketcasts.profile.cloud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.coroutines.flow.combine
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SettingsFilesAutoAddUpNextToggledEvent
import com.automattic.eventhorizon.SettingsFilesAutoDownloadFromCloudToggledEvent
import com.automattic.eventhorizon.SettingsFilesAutoUploadToCloudToggledEvent
import com.automattic.eventhorizon.SettingsFilesDeleteCloudFileAfterPlayingToggledEvent
import com.automattic.eventhorizon.SettingsFilesDeleteLocalFileAfterPlayingToggledEvent
import com.automattic.eventhorizon.SettingsFilesOnlyOnWifiToggledEvent
import com.automattic.eventhorizon.SettingsFilesShownEvent
import com.automattic.eventhorizon.UpgradeBannerDismissedEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.reactive.asFlow

@HiltViewModel
class CloudSettingsViewModel @Inject constructor(
    private val eventHorizon: EventHorizon,
    private val settings: Settings,
    private val userManager: UserManager,
) : ViewModel() {

    private var isFragmentChangingConfigurations: Boolean = false

    private val isUpgradeBannerDismissed = MutableStateFlow(settings.getUpgradeClosedCloudSettings())

    val uiState: StateFlow<UiState> = combine(
        settings.cloudAddToUpNext.flow,
        settings.deleteLocalFileAfterPlaying.flow,
        settings.deleteCloudFileAfterPlaying.flow,
        settings.cloudAutoUpload.flow,
        settings.cloudAutoDownload.flow,
        settings.cloudDownloadOnlyOnWifi.flow,
        userManager.getSignInState().asFlow(),
        isUpgradeBannerDismissed,
        transform = { addToUpNext, deleteLocal, deleteCloud, autoUpload, autoDownload, onlyOnWifi, signInState, bannerDismissed ->
            val isSignedInAsPlusOrPatron = signInState.isSignedInAsPlusOrPatron
            UiState(
                cloudAddToUpNext = addToUpNext,
                deleteLocalFileAfterPlaying = deleteLocal,
                deleteCloudFileAfterPlaying = deleteCloud,
                cloudAutoUpload = autoUpload,
                cloudAutoDownload = autoDownload,
                cloudDownloadOnlyOnWifi = onlyOnWifi,
                isSignedInAsPlusOrPatron = isSignedInAsPlusOrPatron,
                isUpgradeBannerVisible = !isSignedInAsPlusOrPatron && !bannerDismissed,
            )
        },
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = UiState(),
    )

    fun onShown() {
        if (!isFragmentChangingConfigurations) {
            eventHorizon.track(SettingsFilesShownEvent)
        }
    }

    fun onFragmentPause(isChangingConfigurations: Boolean?) {
        isFragmentChangingConfigurations = isChangingConfigurations ?: false
    }

    fun setAddToUpNext(enabled: Boolean) {
        settings.cloudAddToUpNext.set(enabled, updateModifiedAt = true)
        eventHorizon.track(
            SettingsFilesAutoAddUpNextToggledEvent(
                enabled = enabled,
            ),
        )
    }

    fun setDeleteLocalFileAfterPlaying(enabled: Boolean) {
        settings.deleteLocalFileAfterPlaying.set(enabled, updateModifiedAt = true)
        eventHorizon.track(
            SettingsFilesDeleteLocalFileAfterPlayingToggledEvent(
                enabled = enabled,
            ),
        )
    }

    fun setDeleteCloudFileAfterPlaying(enabled: Boolean) {
        settings.deleteCloudFileAfterPlaying.set(enabled, updateModifiedAt = true)
        eventHorizon.track(
            SettingsFilesDeleteCloudFileAfterPlayingToggledEvent(
                enabled = enabled,
            ),
        )
    }

    fun setCloudAutoUpload(enabled: Boolean) {
        settings.cloudAutoUpload.set(enabled, updateModifiedAt = true)
        eventHorizon.track(
            SettingsFilesAutoUploadToCloudToggledEvent(
                enabled = enabled,
            ),
        )
    }

    fun setCloudAutoDownload(enabled: Boolean) {
        settings.cloudAutoDownload.set(enabled, updateModifiedAt = true)
        eventHorizon.track(
            SettingsFilesAutoDownloadFromCloudToggledEvent(
                enabled = enabled,
            ),
        )
    }

    fun setCloudOnlyWifi(enabled: Boolean) {
        settings.cloudDownloadOnlyOnWifi.set(enabled, updateModifiedAt = true)
        eventHorizon.track(
            SettingsFilesOnlyOnWifiToggledEvent(
                enabled = enabled,
            ),
        )
    }

    fun onUpgradeBannerDismissed(source: SourceView) {
        settings.setUpgradeClosedCloudSettings(true)
        isUpgradeBannerDismissed.value = true
        eventHorizon.track(
            UpgradeBannerDismissedEvent(
                source = source.analyticsValue,
            ),
        )
    }

    data class UiState(
        val cloudAddToUpNext: Boolean = false,
        val deleteLocalFileAfterPlaying: Boolean = false,
        val deleteCloudFileAfterPlaying: Boolean = false,
        val cloudAutoUpload: Boolean = false,
        val cloudAutoDownload: Boolean = false,
        val cloudDownloadOnlyOnWifi: Boolean = false,
        val isSignedInAsPlusOrPatron: Boolean = false,
        val isUpgradeBannerVisible: Boolean = false,
    )
}
