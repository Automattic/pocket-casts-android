package au.com.shiftyjelly.pocketcasts.profile.cloud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.file.CloudFilesManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.views.helper.CloudDeleteHelper
import au.com.shiftyjelly.pocketcasts.views.helper.DeleteState
import au.com.shiftyjelly.pocketcasts.views.helper.EpisodeItemTouchHelper.SwipeAction
import au.com.shiftyjelly.pocketcasts.views.helper.EpisodeItemTouchHelper.SwipeSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CloudFilesViewModel @Inject constructor(
    private val userEpisodeManager: UserEpisodeManager,
    private val playbackManager: PlaybackManager,
    private val episodeManager: EpisodeManager,
    private val settings: Settings,
    userManager: UserManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val episodeAnalytics: EpisodeAnalytics,
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

    fun getDeleteStateOnSwipeDelete(episode: UserEpisode): DeleteState {
        trackSwipeAction(SwipeAction.DELETE)
        return CloudDeleteHelper.getDeleteState(episode)
    }

    fun deleteEpisode(episode: UserEpisode, deleteState: DeleteState) {
        CloudDeleteHelper.deleteEpisode(episode, deleteState, playbackManager, episodeManager, userEpisodeManager)
        episodeAnalytics.trackEvent(
            event = if (deleteState == DeleteState.Cloud && !episode.isDownloaded) AnalyticsEvent.EPISODE_DELETED_FROM_CLOUD else AnalyticsEvent.EPISODE_DOWNLOAD_DELETED,
            source = SourceView.FILES,
            uuid = episode.uuid,
        )
    }

    fun changeSort(sortOrder: Settings.CloudSortOrder) {
        settings.setCloudSortOrder(sortOrder)
        cloudFilesManager.sortOrderRelay.accept(sortOrder)
    }

    fun getSortOrder(): Settings.CloudSortOrder {
        return cloudFilesManager.sortOrderRelay.value ?: settings.getCloudSortOrder()
    }

    private fun trackSwipeAction(swipeAction: SwipeAction) {
        analyticsTracker.track(
            AnalyticsEvent.EPISODE_SWIPE_ACTION_PERFORMED,
            mapOf(
                ACTION_KEY to swipeAction.analyticsValue,
                SOURCE_KEY to SwipeSource.FILES.analyticsValue
            )
        )
    }

    companion object {
        private const val ACTION_KEY = "action"
        private const val SOURCE_KEY = "source"
    }
}
