package au.com.shiftyjelly.pocketcasts.repositories.download

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutoDownloadEpisodeProvider @Inject constructor(
    private val podcastManager: PodcastManager,
    private val episodeManager: EpisodeManager,
    private val playlistManager: PlaylistManager,
    private val upNextQueue: UpNextQueue,
    private val userEpisodeManager: UserEpisodeManager,
    private val settings: Settings,
) {
    // Set dispatcher to default because some users follow thousands of podcasts
    suspend fun getAll(newPodcastEpisodeUuids: Collection<String>) = withContext(Dispatchers.Default) {
        buildSet {
            addAll(getPodcastEpisodes(newPodcastEpisodeUuids))
            addAll(getPlaylistEpisodes())
            addAll(getUpNextAutoEpisodes())
            addAll(getUserEpisodes())
        }
    }

    private suspend fun getPodcastEpisodes(newEpisodeUuids: Collection<String>): Set<String> {
        val globalEnabled = settings.autoDownloadNewEpisodes.value == Podcast.AUTO_DOWNLOAD_NEW_EPISODES
        return if (globalEnabled) {
            val perPodcastLimit = settings.autoDownloadLimit.value.episodeCount
            val newEpisodeUuidSet = newEpisodeUuids.toSet()
            podcastManager
                .findSubscribedNoOrder()
                .asSequence()
                .filter(Podcast::isAutoDownloadNewEpisodes)
                .flatMapTo(mutableSetOf()) { podcast ->
                    episodeManager
                        .findEpisodesByPodcastOrderedSuspend(podcast)
                        .asSequence()
                        .filter(BaseEpisode::canQueueForAutoDownload)
                        .map(BaseEpisode::uuid)
                        .filter(newEpisodeUuidSet::contains)
                        .take(perPodcastLimit)
                }
        } else {
            emptySet()
        }
    }

    private suspend fun getPlaylistEpisodes(): Set<String> {
        return playlistManager
            .getAutoDownloadPlaylists()
            .flatMapTo(mutableSetOf()) { playlist ->
                playlist.episodes
                    .asSequence()
                    .mapNotNull(PlaylistEpisode::toPodcastEpisode)
                    .filter(BaseEpisode::canQueueForAutoDownload)
                    .map(BaseEpisode::uuid)
                    .take(playlist.settings.autoDownloadLimit)
            }
    }

    private fun getUpNextAutoEpisodes(): Set<String> {
        return if (settings.autoDownloadUpNext.value) {
            upNextQueue.allEpisodes
                .asSequence()
                .filter(BaseEpisode::canQueueForAutoDownload)
                .map(BaseEpisode::uuid)
                .toSet()
        } else {
            emptySet()
        }
    }

    private suspend fun getUserEpisodes(): Set<String> {
        return if (settings.cloudAutoDownload.value && settings.cachedSubscription.value != null) {
            userEpisodeManager
                .findUserEpisodes()
                .asSequence()
                .filter(BaseEpisode::canQueueForAutoDownload)
                .map(BaseEpisode::uuid)
                .toSet()
        } else {
            emptySet()
        }
    }
}
