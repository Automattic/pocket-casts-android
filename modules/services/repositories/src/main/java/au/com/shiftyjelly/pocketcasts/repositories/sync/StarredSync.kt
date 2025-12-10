package au.com.shiftyjelly.pocketcasts.repositories.sync

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import com.pocketcasts.service.api.StarredEpisode
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.rx2.await
import timber.log.Timber

/**
 * Sync starred episodes from the server.
 */
class StarredSync @Inject constructor(
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
    private val settings: Settings,
) {
    companion object {
        private val ONE_WEEK_MS = TimeUnit.DAYS.toMillis(7)
    }

    suspend fun syncStarredEpisodes(
        serverEpisodes: List<StarredEpisode>,
        currentTimeMs: Long = System.currentTimeMillis(),
    ) {
        val lastStarredModified = settings.getStarredServerModified()

        // Ignore older episodes we have already processed or missing episode will cause lots of server calls and a slow sync
        // Include all episodes starred in the last week to handle slow syncing devices
        val oneWeekAgo = currentTimeMs - ONE_WEEK_MS
        val serverEpisodesFiltered = serverEpisodes.filter { episode ->
            episode.starredModified >= oneWeekAgo || episode.starredModified > lastStarredModified
        }

        Timber.i("StarredSync - processing ${serverEpisodesFiltered.size} of ${serverEpisodes.size} episodes")

        var maxStarredModified = lastStarredModified
        serverEpisodesFiltered.forEach { serverEpisode ->
            processServerEpisode(serverEpisode)
            if (serverEpisode.starredModified > maxStarredModified) {
                maxStarredModified = serverEpisode.starredModified
            }
        }

        // Update the last sync time
        if (maxStarredModified > lastStarredModified) {
            settings.setStarredServerModified(maxStarredModified)
        }
    }

    private suspend fun processServerEpisode(serverEpisode: StarredEpisode) {
        val podcastUuid = serverEpisode.podcastUuid
        val episodeUuid = serverEpisode.uuid

        // Import missing podcast
        val podcast = podcastManager.findOrDownloadPodcastRxSingle(podcastUuid).await() ?: return

        // Import missing episodes
        var localEpisode = episodeManager.findByUuid(episodeUuid)

        // Podcasts not followed aren't kept up to date, so we need to download the episode
        if (localEpisode == null && !podcast.isSubscribed) {
            localEpisode = episodeManager.downloadMissingPodcastEpisode(
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
            )
        }

        if (localEpisode == null) {
            Timber.i("StarredSync - episode not found: $episodeUuid")
            return
        }

        // Check if the episode has been starred more recently locally
        val localLastStarredDate = localEpisode.lastStarredDate
        if (localLastStarredDate != null && localLastStarredDate >= serverEpisode.starredModified) {
            return
        }

        // Sync starred state
        if (!localEpisode.isStarred || localEpisode.lastStarredDate != serverEpisode.starredModified) {
            episodeManager.starEpisodeFromServer(
                episode = localEpisode,
                modified = serverEpisode.starredModified,
            )
        }
    }
}
