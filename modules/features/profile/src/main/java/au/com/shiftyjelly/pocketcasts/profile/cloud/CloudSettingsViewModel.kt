package au.com.shiftyjelly.pocketcasts.profile.cloud

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
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

@HiltViewModel
class CloudSettingsViewModel @Inject constructor(
    private val eventHorizon: EventHorizon,
    private val settings: Settings,
    userManager: UserManager,
) : ViewModel() {

    val signInState: LiveData<SignInState> = userManager.getSignInState().toLiveData()

    private var isFragmentChangingConfigurations: Boolean = false

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
        eventHorizon.track(
            UpgradeBannerDismissedEvent(
                source = source.analyticsValue,
            ),
        )
    }
}
