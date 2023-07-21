package au.com.shiftyjelly.pocketcasts.profile.cloud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.file.CloudFilesManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CloudFilesViewModel @Inject constructor(
    private val userEpisodeManager: UserEpisodeManager,
    private val playbackManager: PlaybackManager,
    private val settings: Settings,
    userManager: UserManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val cloudFilesManager: CloudFilesManager,
) : ViewModel() {

    val accountUsage = userEpisodeManager.observeAccountUsage().toLiveData()
    val signInState = userManager.getSignInState().toLiveData()
    val cloudFilesList = cloudFilesManager.cloudFilesList

    fun refreshFiles(userInitiated: Boolean) {
        if (userInitiated) {
            analyticsTracker.track(
                AnalyticsEvent.PULLED_TO_REFRESH,
                mapOf(
                    "source" to when (cloudFilesManager.cloudFilesList.value?.isEmpty()) {
                        true -> "no_files"
                        false -> "files"
                        else -> "unknown"
                    }
                )
            )
        }
        userEpisodeManager.syncFilesInBackground(playbackManager)
    }

    fun changeSort(sortOrder: Settings.CloudSortOrder) {
        settings.setCloudSortOrder(sortOrder)
        cloudFilesManager.sortOrderRelay.accept(sortOrder)
    }

    fun getSortOrder(): Settings.CloudSortOrder {
        return cloudFilesManager.sortOrderRelay.value ?: settings.getCloudSortOrder()
    }

    companion object {
        private const val ACTION_KEY = "action"
        private const val SOURCE_KEY = "source"
    }
}
