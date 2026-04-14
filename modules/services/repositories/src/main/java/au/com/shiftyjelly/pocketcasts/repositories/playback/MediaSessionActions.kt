package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.toPodcastEpisodes
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.eventhorizon.EpisodeArchivedEvent
import com.automattic.eventhorizon.EpisodeMarkedAsPlayedEvent
import com.automattic.eventhorizon.EventHorizon
import io.reactivex.Completable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shared action methods used by both the legacy [MediaSessionManager.MediaSessionCallback]
 * and the new [Media3SessionCallback]. Reduces duplication across the two callback
 * implementations during the Media3 migration.
 */
internal class MediaSessionActions(
    private val playbackManager: PlaybackManager,
    private val podcastManager: PodcastManager,
    private val episodeManager: EpisodeManager,
    private val playlistManager: PlaylistManager,
    private val settings: Settings,
    private val eventHorizon: EventHorizon,
    private val scopeProvider: () -> CoroutineScope,
    private val source: SourceView = SourceView.MEDIA_BUTTON_BROADCAST_ACTION,
    private val onSearchFailed: ((String) -> Unit)? = null,
) {

    fun markAsPlayed() {
        scopeProvider().launch { markAsPlayedSuspend() }
    }

    suspend fun markAsPlayedSuspend() {
        val episode = playbackManager.getCurrentEpisode()
        episodeManager.markAsPlayedBlocking(episode, playbackManager, podcastManager)
        episode?.let {
            eventHorizon.track(
                EpisodeMarkedAsPlayedEvent(
                    episodeUuid = it.uuid,
                    source = source.analyticsValue,
                ),
            )
        }
    }

    fun starEpisode() {
        scopeProvider().launch { starEpisodeSuspend() }
    }

    suspend fun starEpisodeSuspend() {
        playbackManager.getCurrentEpisode()?.let {
            if (it is PodcastEpisode) {
                it.isStarred = true
                episodeManager.starEpisode(episode = it, starred = true, sourceView = source)
            }
        }
    }

    fun unstarEpisode() {
        scopeProvider().launch { unstarEpisodeSuspend() }
    }

    suspend fun unstarEpisodeSuspend() {
        playbackManager.getCurrentEpisode()?.let {
            if (it is PodcastEpisode) {
                it.isStarred = false
                episodeManager.starEpisode(episode = it, starred = false, sourceView = source)
            }
        }
    }

    fun changePlaybackSpeed() {
        scopeProvider().launch { changePlaybackSpeedSuspend() }
    }

    suspend fun changePlaybackSpeedSuspend() {
        val newSpeed = when (playbackManager.getPlaybackSpeed()) {
            in 0.0..<0.60 -> 0.6
            in 0.60..<0.80 -> 0.8
            in 0.80..<1.00 -> 1.0
            in 1.00..<1.20 -> 1.2
            in 1.20..<1.40 -> 1.4
            in 1.40..<1.60 -> 1.6
            in 1.60..<1.80 -> 1.8
            in 1.80..<2.00 -> 2.0
            in 2.00..<3.00 -> 3.0
            in 3.00..<3.05 -> 0.6
            else -> 1.0
        }

        val episode = playbackManager.getCurrentEpisode() ?: return
        if (episode is PodcastEpisode) {
            val podcast = podcastManager.findPodcastByUuid(episode.podcastUuid)
            if (podcast != null && podcast.overrideGlobalEffects) {
                podcast.playbackSpeed = newSpeed
                podcastManager.updatePlaybackSpeedBlocking(podcast = podcast, speed = newSpeed)
                playbackManager.updatePlayerEffects(effects = podcast.playbackEffects)
                return
            }
        }
        val effects = settings.globalPlaybackEffects.value
        effects.playbackSpeed = newSpeed
        settings.globalPlaybackEffects.set(effects, updateModifiedAt = true)
        playbackManager.updatePlayerEffects(effects = effects)
    }

    fun archive() {
        scopeProvider().launch { archiveSuspend() }
    }

    suspend fun archiveSuspend() {
        playbackManager.getCurrentEpisode()?.let {
            if (it is PodcastEpisode) {
                it.isArchived = true
                episodeManager.archiveBlocking(it, playbackManager)
                eventHorizon.track(
                    EpisodeArchivedEvent(
                        episodeUuid = it.uuid,
                        source = source.analyticsValue,
                    ),
                )
            }
        }
    }

    fun performPlayFromSearchRx(searchTerm: String?): Completable {
        return Completable.fromAction { performPlayFromSearch(searchTerm) }
    }

    fun performPlayFromSearch(searchTerm: String?) {
        scopeProvider().launch { performPlayFromSearchSuspend(searchTerm) }
    }

    suspend fun performPlayFromSearchSuspend(searchTerm: String?) {
        val query: String = searchTerm?.trim()?.lowercase() ?: return

        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "performPlayFromSearch query: %s", query)

        val sourceView = SourceView.MEDIA_BUTTON_BROADCAST_SEARCH_ACTION

        if (query.startsWith("up next")) {
            playbackManager.playQueue(sourceView = sourceView)
            return
        }

        if (query.startsWith("next episode") || query.startsWith("next podcast")) {
            val queueEpisodes = playbackManager.upNextQueue.queueEpisodes
            queueEpisodes.firstOrNull()?.let { episode ->
                playbackManager.playNext(episode = episode, source = sourceView)
                return
            }
        }

        val options = MediaSessionManager.calculateSearchQueryOptions(query)
        for (option in options) {
            val matchingPodcast: Podcast? = podcastManager.searchPodcastByTitleBlocking(option)
            if (matchingPodcast != null) {
                LogBuffer.i(LogBuffer.TAG_PLAYBACK, "User played podcast from search %s.", option)
                playPodcast(podcast = matchingPodcast, sourceView = sourceView)
                return
            }
        }

        for (option in options) {
            val matchingEpisode = episodeManager.findFirstBySearchQuery(option) ?: continue
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "User played episode from search %s.", option)
            playbackManager.playNow(episode = matchingEpisode, sourceView = sourceView)
            return
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
                return
            }
        }

        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "No search results for: %s", query)
        onSearchFailed?.invoke("No search results")
    }

    fun findPodcast(episode: PodcastEpisode): Podcast? {
        return podcastManager.findPodcastByUuidBlocking(episode.podcastUuid)
    }

    private suspend fun playPodcast(podcast: Podcast, sourceView: SourceView) {
        val latestEpisode = withContext(Dispatchers.Default) {
            episodeManager.findLatestUnfinishedEpisodeByPodcastBlocking(podcast)
        } ?: return
        playbackManager.playNow(episode = latestEpisode, sourceView = sourceView)
    }
}
