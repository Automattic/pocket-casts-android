package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject

@HiltViewModel
class EpisodeChipViewModel @Inject constructor(
    private val episodeManager: EpisodeManager,
    playbackManager: PlaybackManager,
    podcastManager: PodcastManager,
) : ViewModel() {

    val upNextQueue: Flow<UpNextQueue.State> = playbackManager
        .upNextQueue
        .getChangesObservableWithLiveCurrentEpisode(episodeManager, podcastManager)
        .asFlow()

    fun observeByUuid(episode: BaseEpisode): StateFlow<BaseEpisode> =
        episodeManager
            .observeEpisodeByUuid(episode.uuid)
            .stateIn(viewModelScope, SharingStarted.Eagerly, episode)
}
