package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.to.HistorySyncResponse
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.extensions.parseIsoDate
import io.reactivex.Maybe
import java.util.Date
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx2.awaitSingleOrNull
import kotlinx.coroutines.withContext
import timber.log.Timber

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
                } else if (podcastUuid != null) {
                    // Add missing podcast and episode
                    podcastManager.findOrDownloadPodcastRx(podcastUuid)
                        .toMaybe()
                        .doOnError { exception -> Timber.e(exception, "Failed to download missing podcast $podcastUuid") }
                        .onErrorResumeNext(Maybe.empty())
                        .awaitSingleOrNull()

                    val skeleton = Episode(
                        uuid = episodeUuid,
                        podcastUuid = podcastUuid,
                        title = change.title ?: "",
                        publishedDate = change.published?.parseIsoDate() ?: Date(),
                        downloadUrl = change.url
                    )
                    val missingEpisode = episodeManager.downloadMissingEpisode(
                        episodeUuid,
                        podcastUuid,
                        skeleton,
                        podcastManager,
                        false
                    )
                        .doOnError { exception -> Timber.e(exception, "Failed to download missing episode $episodeUuid for podcast $podcastUuid") }
                        .onErrorResumeNext(Maybe.empty())
                        .awaitSingleOrNull()
                    if (missingEpisode != null && missingEpisode is Episode) {
                        missingEpisode.lastPlaybackInteraction = interactionDate
                        episodeManager.update(missingEpisode)
                    }
                }
            } else if (change.action == ACTION_DELETE) {
                if (episode != null) {
                    episode.lastPlaybackInteraction = 0
                    episode.lastPlaybackInteractionSyncStatus = 1
                    episodeManager.update(episode)
                }
            }
        }

        if (updateServerModified) {
            settings.setHistoryServerModified(response.serverModified)
        }
    }
}
