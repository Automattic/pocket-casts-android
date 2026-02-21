package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.rx2.asFlow

@HiltViewModel
class UpNextViewModel @Inject constructor(
    episodeManager: EpisodeManager,
    podcastManager: PodcastManager,
    playbackManager: PlaybackManager,
    settings: Settings,
) : ViewModel() {

    val upNextQueue: Flow<UpNextQueue.State> = playbackManager.upNextQueue
        .getChangesObservableWithLiveCurrentEpisode(episodeManager, podcastManager)
        .asFlow()

    val artworkConfiguration = settings.artworkConfiguration.flow
}
