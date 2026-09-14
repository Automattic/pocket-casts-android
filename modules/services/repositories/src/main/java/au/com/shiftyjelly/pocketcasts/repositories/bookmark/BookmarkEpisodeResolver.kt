package au.com.shiftyjelly.pocketcasts.repositories.bookmark

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import javax.inject.Inject
import kotlinx.coroutines.rx2.await

class BookmarkEpisodeResolver @Inject constructor(
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
) {
    suspend fun resolve(bookmark: Bookmark): BaseEpisode? {
        episodeManager.findEpisodeByUuid(bookmark.episodeUuid)?.let { return it }
        val podcast = runCatching { podcastManager.findOrDownloadPodcastRxSingle(bookmark.podcastUuid).await() }.getOrNull() ?: return null
        return if (!podcast.isSubscribed) {
            episodeManager.downloadMissingPodcastEpisode(bookmark.episodeUuid, bookmark.podcastUuid)
        } else {
            episodeManager.findEpisodeByUuid(bookmark.episodeUuid)
        }
    }
}
