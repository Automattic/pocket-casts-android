package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpNextViewModel @Inject constructor(
    episodeManager: EpisodeManager,
    podcastManager: PodcastManager,
    private val playbackManager: PlaybackManager,
) : ViewModel() {

    val upNextQueue: Observable<UpNextQueue.State> = playbackManager.upNextQueue.getChangesObservableWithLiveCurrentEpisode(episodeManager, podcastManager)

    fun play(playable: Playable) {
        viewModelScope.launch {
            playbackManager.playNow(
                episode = playable,
                playbackSource = AnalyticsSource.WATCH_UP_NEXT,
            )
        }
    }
}
