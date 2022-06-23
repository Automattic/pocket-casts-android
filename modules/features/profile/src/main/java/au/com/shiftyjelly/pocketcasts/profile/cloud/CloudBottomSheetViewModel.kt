package au.com.shiftyjelly.pocketcasts.profile.cloud

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun getDeleteState(episode: UserEpisode): DeleteState {
        return CloudDeleteHelper.getDeleteState(episode)
    }

    fun deleteEpisode(episode: UserEpisode, deleteState: DeleteState) {
        CloudDeleteHelper.deleteEpisode(episode, deleteState, playbackManager, episodeManager, userEpisodeManager)
    }

    fun uploadEpisode(episode: UserEpisode) {
        userEpisodeManager.uploadToServer(episode, waitForWifi = false)
    }

    fun removeEpisode(episode: UserEpisode) {
        userEpisodeManager.removeFromCloud(episode)
    }

    fun cancelUpload(episode: UserEpisode) {
        userEpisodeManager.cancelUpload(episode)
    }

    fun download(episode: UserEpisode) {
        viewModelScope.launch(Dispatchers.Default) {
            DownloadHelper.manuallyDownloadEpisodeNow(episode, "cloud bottom sheet", downloadManager, episodeManager)
        }
    }

    fun removeFromUpNext(episode: UserEpisode) {
        playbackManager.removeEpisode(episode)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun playNext(episode: UserEpisode) {
        GlobalScope.launch(Dispatchers.Default) {
            playbackManager.playNext(episode)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun playLast(episode: UserEpisode) {
        GlobalScope.launch(Dispatchers.Default) {
            playbackManager.playLast(episode)
        }
    }

    fun markAsPlayed(episode: UserEpisode) {
        viewModelScope.launch(Dispatchers.Default) {
            episodeManager.markAsPlayed(episode, playbackManager, podcastManager)
        }
    }

    fun markAsUnplayed(episode: UserEpisode) {
        viewModelScope.launch(Dispatchers.Default) {
            episodeManager.markAsNotPlayed(episode)
        }
    }

    fun playNow(episode: UserEpisode, forceStream: Boolean) {
        playbackManager.playNow(episode, forceStream)
    }

    fun pause() {
        playbackManager.pause()
    }
}

data class BottomSheetState(
    val episode: UserEpisode,
    val inUpNext: Boolean,
    val isPlaying: Boolean
)
