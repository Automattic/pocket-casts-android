package au.com.shiftyjelly.pocketcasts.profile.cloud

import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.views.helper.CloudDeleteHelper
import au.com.shiftyjelly.pocketcasts.views.helper.DeleteState
import com.jakewharton.rxrelay2.BehaviorRelay
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CloudFilesViewModel @Inject constructor(
    private val userEpisodeManager: UserEpisodeManager,
    private val playbackManager: PlaybackManager,
    private val episodeManager: EpisodeManager,
    private val settings: Settings,
    userManager: UserManager,
) : ViewModel() {

    val sortOrderRelay = BehaviorRelay.create<Settings.CloudSortOrder>().apply { accept(settings.getCloudSortOrder()) }
    val sortedCloudFiles = sortOrderRelay.toFlowable(BackpressureStrategy.LATEST).switchMap { userEpisodeManager.observeUserEpisodesSorted(it) }
    val cloudFilesList = LiveDataReactiveStreams.fromPublisher(sortedCloudFiles)
    val accountUsage = LiveDataReactiveStreams.fromPublisher(userEpisodeManager.observeAccountUsage())
    val signInState = LiveDataReactiveStreams.fromPublisher(userManager.getSignInState())

    fun refreshFiles() {
        userEpisodeManager.syncFilesInBackground(playbackManager)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun episodeSwipeUpNext(episode: Playable) {
        GlobalScope.launch(Dispatchers.Default) {
            if (playbackManager.upNextQueue.contains(episode.uuid)) {
                playbackManager.removeEpisode(episode)
            } else {
                playbackManager.playNext(episode)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun episodeSwipeUpLast(episode: Playable) {
        GlobalScope.launch(Dispatchers.Default) {
            if (playbackManager.upNextQueue.contains(episode.uuid)) {
                playbackManager.removeEpisode(episode)
            } else {
                playbackManager.playLast(episode)
            }
        }
    }

    fun getDeleteState(episode: UserEpisode): DeleteState {
        return CloudDeleteHelper.getDeleteState(episode)
    }

    fun deleteEpisode(episode: UserEpisode, deleteState: DeleteState) {
        CloudDeleteHelper.deleteEpisode(episode, deleteState, playbackManager, episodeManager, userEpisodeManager)
    }

    fun changeSort(sortOrder: Settings.CloudSortOrder) {
        settings.setCloudSortOrder(sortOrder)
        sortOrderRelay.accept(sortOrder)
    }

    fun getSortOrder(): Settings.CloudSortOrder {
        return sortOrderRelay.value ?: settings.getCloudSortOrder()
    }
}
