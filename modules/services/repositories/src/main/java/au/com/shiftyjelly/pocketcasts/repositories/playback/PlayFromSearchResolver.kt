package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.toPodcastEpisodes
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Resolves a free-text search such as "play the daily" to something playable and starts it.
 * Used by voice search, Android Auto and AppFunctions.
 */
class PlayFromSearchResolver @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val podcastManager: PodcastManager,
    private val episodeManager: EpisodeManager,
    private val playlistManager: PlaylistManager,
) {
    suspend fun playFromSearch(searchTerm: String, sourceView: SourceView): PlayFromSearchResult {
        val query = searchTerm.trim().lowercase()

        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "performPlayFromSearch query: %s", query)

        if (query.startsWith("up next")) {
            playbackManager.playQueue(sourceView = sourceView)
            return PlayFromSearchResult.UpNext
        }

        if (query.startsWith("next episode") || query.startsWith("next podcast")) {
            val nextEpisode = playbackManager.upNextQueue.queueEpisodes.firstOrNull()
            if (nextEpisode != null) {
                playbackManager.playNext(episode = nextEpisode, source = sourceView)
                return PlayFromSearchResult.Episode(nextEpisode)
            }
        }

        val options = MediaSessionManager.calculateSearchQueryOptions(query)
        for (option in options) {
            val matchingPodcast = podcastManager.searchPodcastByTitleBlocking(option) ?: continue
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "User played podcast from search %s.", option)
            return playPodcast(podcast = matchingPodcast, sourceView = sourceView)
        }

        for (option in options) {
            val matchingEpisode = episodeManager.findFirstBySearchQuery(option) ?: continue
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "User played episode from search %s.", option)
            playbackManager.playNow(episode = matchingEpisode, sourceView = sourceView)
            return PlayFromSearchResult.Episode(matchingEpisode)
        }

        val playlistPreviews = playlistManager.playlistPreviewsFlow().first()
        for (option in options) {
            val playlistPreview = playlistPreviews.find { it.title.equals(option, ignoreCase = true) }
                ?: continue
            val playlist = playlistManager.smartPlaylistFlow(playlistPreview.uuid).first()
                ?: playlistManager.manualPlaylistFlow(playlistPreview.uuid).first()
                ?: continue
            val episodes = playlist.episodes.toPodcastEpisodes()
            if (episodes.isNotEmpty()) {
                LogBuffer.i(LogBuffer.TAG_PLAYBACK, "User played playlist from search %s.", option)
                playbackManager.playEpisodes(episodes = episodes, sourceView = sourceView)
                return PlayFromSearchResult.Playlist(title = playlistPreview.title, firstEpisode = episodes.first())
            }
        }

        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "No search results for: %s", query)
        return PlayFromSearchResult.NotFound
    }

    private suspend fun playPodcast(podcast: Podcast, sourceView: SourceView): PlayFromSearchResult {
        val latestEpisode = withContext(Dispatchers.Default) {
            episodeManager.findLatestUnfinishedEpisodeByPodcastBlocking(podcast)
        } ?: return PlayFromSearchResult.PodcastWithNothingToPlay(podcast)
        playbackManager.playNow(episode = latestEpisode, sourceView = sourceView)
        return PlayFromSearchResult.Episode(latestEpisode)
    }
}

sealed interface PlayFromSearchResult {
    data object UpNext : PlayFromSearchResult

    data class Episode(val episode: BaseEpisode) : PlayFromSearchResult

    data class Playlist(val title: String, val firstEpisode: BaseEpisode) : PlayFromSearchResult

    // The podcast matched but every episode is finished, so nothing was played.
    data class PodcastWithNothingToPlay(val podcast: Podcast) : PlayFromSearchResult

    data object NotFound : PlayFromSearchResult
}
