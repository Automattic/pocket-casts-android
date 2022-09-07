package au.com.shiftyjelly.pocketcasts.profile.cloud

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
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
        trackOptionTapped(DELETE)
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
        trackOptionTapped(DELETE_FROM_CLOUD)
    }

    fun cancelUpload(episode: UserEpisode) {
        userEpisodeManager.cancelUpload(episode)
        trackOptionTapped(CANCEL_UPLOAD)
    }

    fun cancelDownload(episode: UserEpisode) {
        downloadManager.removeEpisodeFromQueue(episode, "cloud bottom sheet")
        trackOptionTapped(CANCEL_DOWNLOAD)
    }

    fun download(episode: UserEpisode) {
        viewModelScope.launch(Dispatchers.Default) {
            DownloadHelper.manuallyDownloadEpisodeNow(episode, "cloud bottom sheet", downloadManager, episodeManager)
        }
    }

    fun removeFromUpNext(episode: UserEpisode) {
        playbackManager.removeEpisode(episode)
        trackOptionTapped(UP_NEXT_DELETE)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun playNext(episode: UserEpisode) {
        GlobalScope.launch(Dispatchers.Default) {
            playbackManager.playNext(episode)
            trackOptionTapped(UP_NEXT_ADD_TOP)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun playLast(episode: UserEpisode) {
        GlobalScope.launch(Dispatchers.Default) {
            playbackManager.playLast(episode)
            trackOptionTapped(UP_NEXT_ADD_BOTTOM)
        }
    }

    fun markAsPlayed(episode: UserEpisode) {
        viewModelScope.launch(Dispatchers.Default) {
            episodeManager.markAsPlayed(episode, playbackManager, podcastManager)
            trackOptionTapped(MARK_PLAYED)
        }
    }

    fun markAsUnplayed(episode: UserEpisode) {
        viewModelScope.launch(Dispatchers.Default) {
            episodeManager.markAsNotPlayed(episode)
            trackOptionTapped(MARK_UNPLAYED)
        }
    }

    fun playNow(episode: UserEpisode, forceStream: Boolean) {
        playbackManager.playNow(episode, forceStream)
        analyticsTracker.track(AnalyticsEvent.USER_FILE_PLAY_PAUSE_BUTTON_TAPPED, mapOf(OPTION_KEY to PLAY))
    }

    fun pause() {
        playbackManager.pause()
        analyticsTracker.track(AnalyticsEvent.USER_FILE_PLAY_PAUSE_BUTTON_TAPPED, mapOf(OPTION_KEY to PAUSE))
    }

    fun trackOptionTapped(option: String) {
        analyticsTracker.track(AnalyticsEvent.USER_FILE_DETAIL_OPTION_TAPPED, mapOf(OPTION_KEY to option))
    }

    companion object {
        private const val OPTION_KEY = "option"
        private const val UP_NEXT_DELETE = "up_next_delete"
        private const val UP_NEXT_ADD_TOP = "up_next_add_top"
        private const val UP_NEXT_ADD_BOTTOM = "up_next_add_bottom"
        private const val MARK_PLAYED = "mark_played"
        private const val MARK_UNPLAYED = "mark_unplayed"
        private const val DELETE = "delete"
        private const val CANCEL_UPLOAD = "cancel_upload"
        private const val CANCEL_DOWNLOAD = "cancel_download"
        private const val DELETE_FROM_CLOUD = "delete_from_cloud"
        private const val PLAY = "play"
        private const val PAUSE = "pause"
        const val EDIT = "edit"
        const val UPLOAD = "upload"
        const val DOWNLOAD = "download"
        const val UPLOAD_UPGRADE_REQUIRED = "upload_upgrade_required"
    }
}

data class BottomSheetState(
    val episode: UserEpisode,
    val inUpNext: Boolean,
    val isPlaying: Boolean
)
