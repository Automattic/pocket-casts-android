package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.db.helper.UserEpisodePodcastSubstitute
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.to.HistorySyncResponse
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.extensions.parseIsoDate
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class HistoryManager @Inject constructor(
    private val podcastManager: PodcastManager,
    private val episodeManager: EpisodeManager,
    private val settings: Settings,
) : CoroutineScope {

    companion object {
        const val ACTION_ADD = 1
        const val ACTION_DELETE = 2
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    /**
     * Read the server listening history response.
     * @param response The server response.
     * @param updateServerModified Set to true when this is latest listening history, rather than part of the user's history.
     */
    suspend fun processServerResponse(response: HistorySyncResponse, updateServerModified: Boolean) = withContext(Dispatchers.IO) {
        if (!response.hasChanged(0) || response.changes.isNullOrEmpty()) {
            return@withContext
        }

        val changes = response.changes ?: return@withContext

        // add the missing podcasts
        val podcastUuids = changes
            .mapNotNull { change -> change.podcast }
            .toSet()
        val databaseSubscribedPodcastUuids = podcastManager.findSubscribedUuids().toHashSet()
        val missingPodcastUuids = podcastUuids.minus(databaseSubscribedPodcastUuids)
        Observable.fromIterable(missingPodcastUuids)
            .observeOn(Schedulers.io())
            .flatMap({ podcastUuid -> podcastManager.addPodcast(podcastUuid = podcastUuid, sync = false, subscribed = false).toObservable() }, true, 5)
            .doOnError { throwable -> LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, throwable, "History manager could not add podcast") }
            .toList()
            .await()

        val skeletonEpisodes = mutableListOf<Episode>()

        for (change in changes) {
            val interactionDate = change.modifiedAt.toLong()

            val episodeUuid = change.episode ?: continue
            val episode = episodeManager.findByUuid(episodeUuid)
            if (change.action == ACTION_ADD) {
                val podcastUuid = change.podcast
                if (episode != null) {
                    if ((episode.lastPlaybackInteraction ?: 0) < interactionDate) {
                        episode.lastPlaybackInteraction = interactionDate
                        episode.lastPlaybackInteractionSyncStatus = Episode.LAST_PLAYBACK_INTERACTION_SYNCED
                        episodeManager.update(episode)
                    }
                } else if (podcastUuid != null && podcastUuid != UserEpisodePodcastSubstitute.uuid) {
                    val skeleton = Episode(
                        uuid = episodeUuid,
                        podcastUuid = podcastUuid,
                        title = change.title ?: "",
                        publishedDate = change.published?.parseIsoDate() ?: Date(),
                        downloadUrl = change.url,
                        lastPlaybackInteraction = interactionDate
                    )
                    skeletonEpisodes.add(skeleton)
                }
            } else if (change.action == ACTION_DELETE) {
                if (episode != null) {
                    episode.lastPlaybackInteraction = 0
                    episode.lastPlaybackInteractionSyncStatus = 1
                    episodeManager.update(episode)
                }
            }
        }

        episodeManager.insert(skeletonEpisodes)

        if (updateServerModified) {
            settings.setHistoryServerModified(response.serverModified)
        }
    }
}
