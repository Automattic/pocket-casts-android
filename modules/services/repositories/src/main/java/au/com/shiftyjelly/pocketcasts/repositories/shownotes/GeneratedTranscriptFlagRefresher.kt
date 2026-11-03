package au.com.shiftyjelly.pocketcasts.repositories.shownotes

import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class GeneratedTranscriptFlagRefresher @Inject constructor(
    private val cacheServiceManager: PodcastCacheServiceManager,
    private val episodeDao: EpisodeDao,
    private val clock: Clock,
) {
    private val mutexes = ConcurrentHashMap<String, Mutex>()
    private val lastRefreshes = ConcurrentHashMap<String, Instant>()

    suspend fun hasGeneratedTranscript(episode: PodcastEpisode): Boolean {
        if (episode.hasGeneratedTranscript) {
            return true
        }
        if (!episode.isWithinGenerationWindow()) {
            return false
        }
        return mutexes.computeIfAbsent(episode.podcastUuid) { Mutex() }.withLock {
            if (shouldRefresh(episode.podcastUuid)) {
                refresh(episode.podcastUuid)
            }
            episodeDao.findByUuid(episode.uuid)?.hasGeneratedTranscript == true
        }
    }

    private fun PodcastEpisode.isWithinGenerationWindow(): Boolean {
        return publishedDate.toInstant() > clock.instant().minus(GENERATION_WINDOW)
    }

    private fun shouldRefresh(podcastUuid: String): Boolean {
        val lastRefresh = lastRefreshes[podcastUuid] ?: return true
        return lastRefresh.plus(REFRESH_INTERVAL) < clock.instant()
    }

    private suspend fun refresh(podcastUuid: String) {
        try {
            val response = cacheServiceManager.getPodcastResponse(podcastUuid)
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Failed to refresh generated transcript flags for podcast $podcastUuid, HTTP ${response.code()}")
                return
            }
            lastRefreshes[podcastUuid] = clock.instant()
            val episodes = body.podcastInfo.episodes.orEmpty()
            val uuids = episodes.filter { it.hasGeneratedTranscript == true }.map { it.uuid }
            episodeDao.markHasGeneratedTranscript(uuids)
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Refreshed generated transcript flags for podcast $podcastUuid, ${uuids.size} episodes have one")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "Failed to refresh generated transcript flags for podcast $podcastUuid")
        }
    }

    private companion object {
        val REFRESH_INTERVAL = 30.minutes.toJavaDuration()
        val GENERATION_WINDOW = 60.days.toJavaDuration()
    }
}
