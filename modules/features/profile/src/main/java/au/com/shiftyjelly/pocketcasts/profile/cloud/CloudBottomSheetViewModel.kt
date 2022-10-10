package au.com.shiftyjelly.pocketcasts.profile.cloud

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsPropValue
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadHelper
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.containsUuid
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.views.helper.CloudDeleteHelper
import au.com.shiftyjelly.pocketcasts.views.helper.DeleteState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.rxkotlin.Flowables
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CloudBottomSheetViewModel @Inject constructor(
    private val userEpisodeManager: UserEpisodeManager,
    private val playbackManager: PlaybackManager,
    private val episodeManager: EpisodeManager,
    private val downloadManager: DownloadManager,
    private val podcastManager: PodcastManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    userManager: UserManager
) : ViewModel() {
    lateinit var state: LiveData<BottomSheetState>
    var signInState = LiveDataReactiveStreams.fromPublisher(userManager.getSignInState())

    fun setup(uuid: String) {
        val isPlayingFlowable = playbackManager.playbackStateRelay.filter { it.episodeUuid == uuid }.map { it.isPlaying }.startWith(false).toFlowable(BackpressureStrategy.LATEST)
        val inUpNextFlowable = playbackManager.upNextQueue.changesObservable.containsUuid(uuid).toFlowable(BackpressureStrategy.LATEST)
        val episodeFlowable = userEpisodeManager.observeEpisode(uuid)
        val combined = Flowables.combineLatest(episodeFlowable, inUpNextFlowable, isPlayingFlowable) { episode, inUpNext, isPlaying ->
            BottomSheetState(episode, inUpNext, isPlaying)
        }
        state = LiveDataReactiveStreams.fromPublisher(combined)
    }

    fun getDeleteStateOnDeleteClick(episode: UserEpisode): DeleteState {
        trackOptionTapped(AnalyticsProp.Value.DELETE)
        return CloudDeleteHelper.getDeleteState(episode)
    }

    fun deleteEpisode(episode: UserEpisode, deleteState: DeleteState) {
        CloudDeleteHelper.deleteEpisode(episode, deleteState, playbackManager, episodeManager, userEpisodeManager)
        analyticsTracker.track(AnalyticsEvent.USER_FILE_DELETED)
    }

    fun uploadEpisode(episode: UserEpisode) {
        userEpisodeManager.uploadToServer(episode, waitForWifi = false)
    }

    fun removeEpisode(episode: UserEpisode) {
        userEpisodeManager.removeFromCloud(episode)
        trackOptionTapped(AnalyticsProp.Value.DELETE_FROM_CLOUD)
    }

    fun cancelUpload(episode: UserEpisode) {
        userEpisodeManager.cancelUpload(episode)
        trackOptionTapped(AnalyticsProp.Value.CANCEL_UPLOAD)
    }

    fun cancelDownload(episode: UserEpisode) {
        downloadManager.removeEpisodeFromQueue(episode, "cloud bottom sheet")
        trackOptionTapped(AnalyticsProp.Value.CANCEL_DOWNLOAD)
    }

    fun download(episode: UserEpisode) {
        viewModelScope.launch(Dispatchers.Default) {
            DownloadHelper.manuallyDownloadEpisodeNow(episode, "cloud bottom sheet", downloadManager, episodeManager)
        }
    }

    fun removeFromUpNext(episode: UserEpisode) {
        playbackManager.removeEpisode(episode)
        trackOptionTapped(AnalyticsProp.Value.UP_NEXT_DELETE)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun playNext(episode: UserEpisode) {
        GlobalScope.launch(Dispatchers.Default) {
            playbackManager.playNext(episode)
            trackOptionTapped(AnalyticsProp.Value.UP_NEXT_ADD_TOP)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun playLast(episode: UserEpisode) {
        GlobalScope.launch(Dispatchers.Default) {
            playbackManager.playLast(episode)
            trackOptionTapped(AnalyticsProp.Value.UP_NEXT_ADD_BOTTOM)
        }
    }

    fun markAsPlayed(episode: UserEpisode) {
        viewModelScope.launch(Dispatchers.Default) {
            episodeManager.markAsPlayed(episode, playbackManager, podcastManager)
            trackOptionTapped(AnalyticsProp.Value.MARK_PLAYED)
        }
    }

    fun markAsUnplayed(episode: UserEpisode) {
        viewModelScope.launch(Dispatchers.Default) {
            episodeManager.markAsNotPlayed(episode)
            trackOptionTapped(AnalyticsProp.Value.MARK_UNPLAYED)
        }
    }

    fun playNow(episode: UserEpisode, forceStream: Boolean) {
        playbackManager.playNow(episode, forceStream)
        analyticsTracker.track(
            AnalyticsEvent.USER_FILE_PLAY_PAUSE_BUTTON_TAPPED,
            mapOf(AnalyticsProp.Key.OPTION to AnalyticsProp.Value.PLAY)
        )
    }

    fun pause() {
        playbackManager.pause()
        analyticsTracker.track(
            AnalyticsEvent.USER_FILE_PLAY_PAUSE_BUTTON_TAPPED,
            mapOf(AnalyticsProp.Key.OPTION to AnalyticsProp.Value.PAUSE)
        )
    }

    fun trackOptionTapped(option: AnalyticsPropValue) {
        analyticsTracker.track(
            AnalyticsEvent.USER_FILE_DETAIL_OPTION_TAPPED,
            mapOf(AnalyticsProp.Key.OPTION to option)
        )
    }

    companion object {
        object AnalyticsProp {
            object Key {
                const val OPTION = "option"
            }
            object Value {
                val PLAY = AnalyticsPropValue("play")
                val PAUSE = AnalyticsPropValue("pause")
                val DELETE = AnalyticsPropValue("delete")
                val CANCEL_UPLOAD = AnalyticsPropValue("cancel_upload")
                val MARK_UNPLAYED = AnalyticsPropValue("mark_unplayed")
                val UP_NEXT_DELETE = AnalyticsPropValue("up_next_delete")
                val UP_NEXT_ADD_TOP = AnalyticsPropValue("up_next_add_top")
                val UP_NEXT_ADD_BOTTOM = AnalyticsPropValue("up_next_add_bottom")
                val MARK_PLAYED = AnalyticsPropValue("mark_played")
                val CANCEL_DOWNLOAD = AnalyticsPropValue("cancel_download")
                val DELETE_FROM_CLOUD = AnalyticsPropValue("delete_from_cloud")

                val EDIT = AnalyticsPropValue("edit")
                val UPLOAD = AnalyticsPropValue("upload")
                val DOWNLOAD = AnalyticsPropValue("download")
                val UPLOAD_UPGRADE_REQUIRED = AnalyticsPropValue("upload_upgrade_required")
            }
        }
    }
}

data class BottomSheetState(
    val episode: UserEpisode,
    val inUpNext: Boolean,
    val isPlaying: Boolean
)
