package au.com.shiftyjelly.pocketcasts.component

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

enum class TvEpisodeActionContext(val source: SourceView) {
    PodcastDetails(SourceView.PODCAST_SCREEN),
    Playlist(SourceView.FILTERS),
    UpNext(SourceView.UP_NEXT),
}

interface TvEpisodeActions {
    fun playNext(episode: PodcastEpisode, source: SourceView)
    fun playLast(episode: PodcastEpisode, source: SourceView)
    fun markAsPlayed(episode: PodcastEpisode)
    fun markAsUnplayed(episode: PodcastEpisode)
    fun archive(episode: PodcastEpisode)
    fun unarchive(episode: PodcastEpisode)
    fun removeFromUpNext(episode: PodcastEpisode, source: SourceView)
}

@HiltViewModel
class TvEpisodeActionsViewModel @Inject constructor(
    private val episodeManager: EpisodeManager,
    private val playbackManager: PlaybackManager,
    private val podcastManager: PodcastManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel(),
    TvEpisodeActions {

    override fun playNext(episode: PodcastEpisode, source: SourceView) {
        viewModelScope.launch(ioDispatcher) {
            playbackManager.playNext(episode = episode, source = source)
        }
    }

    override fun playLast(episode: PodcastEpisode, source: SourceView) {
        viewModelScope.launch(ioDispatcher) {
            playbackManager.playLast(episode = episode, source = source)
        }
    }

    override fun markAsPlayed(episode: PodcastEpisode) {
        viewModelScope.launch(ioDispatcher) {
            episodeManager.markAsPlayedBlocking(episode, playbackManager, podcastManager)
        }
    }

    override fun markAsUnplayed(episode: PodcastEpisode) {
        viewModelScope.launch(ioDispatcher) {
            episodeManager.markAsNotPlayedBlocking(episode)
        }
    }

    override fun archive(episode: PodcastEpisode) {
        viewModelScope.launch(ioDispatcher) {
            episodeManager.archiveBlocking(episode, playbackManager)
        }
    }

    override fun unarchive(episode: PodcastEpisode) {
        viewModelScope.launch(ioDispatcher) {
            episodeManager.unarchiveBlocking(episode)
        }
    }

    override fun removeFromUpNext(episode: PodcastEpisode, source: SourceView) {
        playbackManager.removeEpisode(episodeToRemove = episode, source = source)
    }
}
