package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.db.helper.UserEpisodePodcastSubstitute
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
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
        const val ADD_PODCAST_CONCURRENCY = 5
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    /**
     * Read the server listening history response.
     * @param response The server response.
     * @param updateServerModified Set to true when this is latest listening history, rather than part of the user's history.
     */
    suspend fun processServerResponse(response: HistorySyncResponse, updateServerModified: Boolean, onProgressChanged: ((Float) -> Unit)? = null) = withContext(Dispatchers.IO) {
        if (!response.hasChanged(0) || response.changes.isNullOrEmpty()) {
            return@withContext
        }

        val changes = response.changes ?: return@withContext

        // all the podcasts that need to be in the database
        val podcastUuids = changes
            .mapNotNull { change -> change.podcast }
            .toSet()
        val databaseSubscribedPodcastUuids = podcastManager.findSubscribedUuids().toHashSet()
        // add the missing podcasts or update the podcast it already unsubscribed in the database
        val missingPodcastUuids = podcastUuids.minus(databaseSubscribedPodcastUuids)

        val total = missingPodcastUuids.size.toFloat()
        var progress = 0

        // add the podcasts five at a time
        Observable.fromIterable(missingPodcastUuids)
            .observeOn(Schedulers.io())
            .flatMap(
                { podcastUuid ->
                    podcastManager.addPodcast(podcastUuid = podcastUuid, sync = false, subscribed = false)
                        .doOnError { throwable -> LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, throwable, "History manager could not add podcast") }
                        .onErrorReturn { Podcast(uuid = podcastUuid) }
                        .toObservable()
                }, true, ADD_PODCAST_CONCURRENCY
            )
            .doOnNext {
                progress += 1
                onProgressChanged?.invoke(progress / total)
            }
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
                    // add episodes which on longer exist in a podcast so they show up in listening history
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
