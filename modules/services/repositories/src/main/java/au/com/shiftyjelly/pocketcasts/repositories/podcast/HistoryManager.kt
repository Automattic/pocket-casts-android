package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.HistorySyncResponse
import au.com.shiftyjelly.pocketcasts.preferences.BuildConfig
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.math.min

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

    private var synced: Double = 0.0
    private var syncTotal: Double = 0.0

    /**
     * Read the server listening history response.
     * @param response The server response.
     * @param updateServerModified Set to true when this is latest listening history, rather than part of the user's history.
     */
    suspend fun processServerResponse(
        response: HistorySyncResponse,
        updateServerModified: Boolean,
        onProgressChanged: ((Float) -> Unit)? = null,
    ) = withContext(Dispatchers.IO) {
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
        syncTotal += total

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
                synced += 1
                // Progress events towards the end can be too close to 100%
                // giving an impression that nothing is happening even when progress is complete,
                // so we cap it at 95% until it's done.
                val progress = min((0.2f + (synced / syncTotal) * 0.8f), 0.95).toFloat()
                if (BuildConfig.DEBUG) {
                    Timber.i("Listening history sync progress: $progress")
                }
                onProgressChanged?.invoke(progress)
            }
            .toList()
            .await()

        val skeletonEpisodes = mutableListOf<PodcastEpisode>()

        for (change in changes) {
            val interactionDate = change.modifiedAt.toLong()

            val episodeUuid = change.episode ?: continue
            val episode = episodeManager.findByUuid(episodeUuid)
            if (change.action == ACTION_ADD) {
                val podcastUuid = change.podcast
                if (episode != null) {
                    if ((episode.lastPlaybackInteraction ?: 0) < interactionDate) {
                        episode.lastPlaybackInteraction = interactionDate
                        episode.lastPlaybackInteractionSyncStatus = PodcastEpisode.LAST_PLAYBACK_INTERACTION_SYNCED
                        episodeManager.update(episode)
                    }
                } else if (podcastUuid != null) {
                    Timber.i("Listening history episode no longer exists. Episode: $episodeUuid podcast: $podcastUuid")
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

    fun resetSyncCount() {
        synced = 0.0
        syncTotal = 0.0
    }
}
