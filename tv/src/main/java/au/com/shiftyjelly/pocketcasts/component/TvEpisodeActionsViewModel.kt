package au.com.shiftyjelly.pocketcasts.component

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

enum class TvEpisodeActionContext(val source: SourceView) {
    PodcastDetails(SourceView.PODCAST_SCREEN),
    SearchResults(SourceView.SEARCH_RESULTS),
    Playlist(SourceView.FILTERS),
    UpNext(SourceView.UP_NEXT),
    NowPlaying(SourceView.PLAYER),
}

interface TvEpisodeActions {
    fun play(episode: PodcastEpisode, source: SourceView)
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
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel(),
    TvEpisodeActions {

    override fun play(episode: PodcastEpisode, source: SourceView) = launchWrite {
        playbackManager.playNowSuspend(episode = episode, sourceView = source)
    }

    override fun playNext(episode: PodcastEpisode, source: SourceView) = launchWrite {
        playbackManager.playNext(episode = episode, source = source)
    }

    override fun playLast(episode: PodcastEpisode, source: SourceView) = launchWrite {
        playbackManager.playLast(episode = episode, source = source)
    }

    override fun markAsPlayed(episode: PodcastEpisode) = launchWrite {
        episodeManager.markAsPlayedBlocking(episode, playbackManager, podcastManager)
    }

    override fun markAsUnplayed(episode: PodcastEpisode) = launchWrite {
        episodeManager.markAsNotPlayedBlocking(episode)
    }

    override fun archive(episode: PodcastEpisode) = launchWrite {
        episodeManager.archiveBlocking(episode, playbackManager)
    }

    override fun unarchive(episode: PodcastEpisode) = launchWrite {
        episodeManager.unarchiveBlocking(episode)
    }

    override fun removeFromUpNext(episode: PodcastEpisode, source: SourceView) {
        playbackManager.removeEpisode(episodeToRemove = episode, source = source)
    }

    private fun launchWrite(block: suspend () -> Unit) {
        applicationScope.launch(ioDispatcher) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "TV episode action failed")
            }
        }
    }
}
