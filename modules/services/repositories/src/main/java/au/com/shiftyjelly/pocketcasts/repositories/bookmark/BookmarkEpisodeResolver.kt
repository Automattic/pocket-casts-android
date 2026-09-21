package au.com.shiftyjelly.pocketcasts.repositories.bookmark

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.rx2.await

class BookmarkEpisodeResolver @Inject constructor(
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
) {
    suspend fun resolve(bookmark: Bookmark) = resolve(
        episodeUuid = bookmark.episodeUuid,
        podcastUuid = bookmark.podcastUuid,
    )

    suspend fun resolve(episodeUuid: String, podcastUuid: String): BaseEpisode? {
        episodeManager.findEpisodeByUuid(episodeUuid)?.let { return it }
        return try {
            // Adding a podcast that wasn't local yet also inserts its feed episodes.
            val podcast = podcastManager.findOrDownloadPodcastRxSingle(podcastUuid).await()
            val localEpisode = episodeManager.findEpisodeByUuid(episodeUuid)
            if (localEpisode == null && !podcast.isSubscribed) {
                episodeManager.downloadMissingPodcastEpisode(episodeUuid, podcastUuid)
            } else {
                localEpisode
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
}
