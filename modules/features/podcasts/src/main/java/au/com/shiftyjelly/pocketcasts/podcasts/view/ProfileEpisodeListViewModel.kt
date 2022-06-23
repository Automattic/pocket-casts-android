package au.com.shiftyjelly.pocketcasts.podcasts.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.math.max

@HiltViewModel
class ProfileEpisodeListViewModel @Inject constructor(val episodeManager: EpisodeManager, val playbackManager: PlaybackManager) : ViewModel(), CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    lateinit var episodeList: LiveData<List<Episode>>

    fun setup(mode: ProfileEpisodeListFragment.Mode) {
        val episodeListFlowable = when (mode) {
            is ProfileEpisodeListFragment.Mode.Downloaded -> episodeManager.observeDownloadEpisodes()
            is ProfileEpisodeListFragment.Mode.Starred -> episodeManager.observeStarredEpisodes()
            is ProfileEpisodeListFragment.Mode.History -> episodeManager.observePlaybackHistoryEpisodes()
        }

        episodeList = LiveDataReactiveStreams.fromPublisher(episodeListFlowable)
    }

    @Suppress("UNUSED_PARAMETER")
    fun episodeSwiped(episode: Playable, index: Int) {
        if (episode !is Episode) return

        launch {
            if (!episode.isArchived) {
                episodeManager.archive(episode, playbackManager)
            } else {
                episodeManager.unarchive(episode)
            }
        }
    }

    fun episodeSwipeUpNext(episode: Playable) {
        launch {
            if (playbackManager.upNextQueue.contains(episode.uuid)) {
                playbackManager.removeEpisode(episode)
            } else {
                playbackManager.playNext(episode)
            }
        }
    }

    fun episodeSwipeUpLast(episode: Playable) {
        launch {
            if (playbackManager.upNextQueue.contains(episode.uuid)) {
                playbackManager.removeEpisode(episode)
            } else {
                playbackManager.playLast(episode)
            }
        }
    }

    fun onArchiveFromHereCount(episode: Episode): Int {
        val episodes = episodeList.value ?: return 0
        val index = max(episodes.indexOf(episode), 0) // -1 on not found
        return episodes.count() - index
    }

    fun clearAllEpisodeHistory() {
        launch {
            episodeManager.clearAllEpisodeHistory()
        }
    }
}
