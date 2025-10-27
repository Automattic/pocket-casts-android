package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.utils.extensions.containsAllInOrderBy
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlayAllHandler @AssistedInject constructor(
    private val playbackManager: PlaybackManager,
    private val playlistManager: PlaylistManager,
    private val settings: Settings,
    private val clock: Clock,
    @Assisted private val source: SourceView,
) {
    @Volatile
    private var pendingEpisodes: PendingEpisodes? = null

    suspend fun handlePlayAllAction(episodes: List<PlaylistEpisode>): PlayAllResponse {
        return handlePlayAllAction(episodes, PlaylistEpisode::toPodcastEpisode)
    }

    private suspend fun <T> handlePlayAllAction(
        episodes: List<T>,
        toBaseEpisode: (T) -> BaseEpisode?,
    ): PlayAllResponse = withContext(Dispatchers.Default) {
        val episodesToPlay = episodes.mapNotNull(toBaseEpisode).take(settings.getMaxUpNextEpisodes())
        val episodesInQueue = playbackManager.upNextQueue.allEpisodes

        when {
            episodesToPlay.isEmpty() -> {
                PlayAllResponse.ShowNoEpisodesToPlay
            }

            episodesToPlay.containsAllInOrderBy(episodesInQueue, BaseEpisode::uuid) -> {
                appendToQueueAndPlay(episodesToPlay.drop(episodesInQueue.size))
                PlayAllResponse.DoNothing
            }

            else -> {
                pendingEpisodes = PendingEpisodes(episodesInQueue, episodesToPlay)
                PlayAllResponse.ShowWarning
            }
        }
    }

    suspend fun saveUpNextAsPlaylist(upNextTranslation: String) {
        val episodes = pendingEpisodes?.episodeInQueue ?: return
        val baseName = buildString {
            append(upNextTranslation)
            runCatching {
                val formatter = DateTimeFormatter.ofPattern("MMMM dd")
                val formattedDate = LocalDate.now(clock).format(formatter)
                append(" - ")
                append(formattedDate)
            }
        }
        val playlistEpisodes = withContext(Dispatchers.Default) {
            episodes
                .filterIsInstance<PodcastEpisode>()
                .chunked(PlaylistManager.MANUAL_PLAYLIST_EPISODE_LIMIT)
                .reversed()
        }
        playlistEpisodes.forEachIndexed { index, episodes ->
            val name = if (index == 0) baseName else "$baseName (${index + 1})"
            playlistManager.createManualPlaylistWithEpisodes(name, episodes)
        }
    }

    suspend fun playAllPendingEpisodes() {
        val episodes = pendingEpisodes?.episodesToPlay ?: return

        withContext(Dispatchers.Default) {
            playbackManager.upNextQueue.removeAll()
            playbackManager.playEpisodes(episodes, source)
        }
    }

    private suspend fun appendToQueueAndPlay(episodes: List<BaseEpisode>) {
        withContext(Dispatchers.Default) {
            playbackManager.addEpisodesLast(episodes, source)
            resumePlayback()
        }
    }

    private fun resumePlayback() {
        if (!playbackManager.isPlaying()) {
            playbackManager.playQueue(source)
        }
    }

    private class PendingEpisodes(
        val episodeInQueue: List<BaseEpisode>,
        val episodesToPlay: List<BaseEpisode>,
    )

    @AssistedFactory
    interface Factory {
        fun create(source: SourceView): PlayAllHandler
    }
}

enum class PlayAllResponse {
    DoNothing,
    ShowNoEpisodesToPlay,
    ShowWarning,
}
