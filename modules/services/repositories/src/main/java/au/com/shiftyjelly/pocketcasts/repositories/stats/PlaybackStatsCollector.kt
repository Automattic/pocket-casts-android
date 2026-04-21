package au.com.shiftyjelly.pocketcasts.repositories.stats

import androidx.collection.LruCache
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.models.db.dao.PlaybackStatsDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PlaybackStatsEvent
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.utils.UUIDProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

interface PlaybackStatsCollector {
    fun onStart()
    fun onStop()
}

@Singleton
class PersistentPlaybackStatsCollector @Inject constructor(
    private val queue: UpNextQueue,
    private val podcastDao: PodcastDao,
    private val playbackStatsDao: PlaybackStatsDao,
    private val clock: Clock,
    private val uuidProvider: UUIDProvider,
    @ApplicationScope private val scope: CoroutineScope,
) : PlaybackStatsCollector {
    private val stateLock = Any()
    private val podcastCache = LruCache<String, Podcast>(maxSize = 20)

    private var isPlaying = false
    private var deferredPartialEvent: Deferred<PartialStatsEvent?>? = null

    override fun onStart() {
        val startedAt = clock.instant()
        synchronized(stateLock) {
            val episode = queue.currentEpisode ?: return
            if (isPlaying) {
                return
            }
            isPlaying = true

            deferredPartialEvent = scope.async {
                val podcast = podcastCache[episode.podcastOrSubstituteUuid] ?: findPodcast(episode)
                podcast?.let {
                    podcastCache.put(podcast.uuid, podcast)
                    PartialStatsEvent(episode, podcast, startedAt)
                }
            }
        }
    }

    override fun onStop() {
        val endedAt = clock.instant()
        val partialEvent = synchronized(stateLock) {
            if (!isPlaying) {
                return
            }
            isPlaying = false

            deferredPartialEvent.also { deferredPartialEvent = null }
        }

        scope.launch {
            val events = partialEvent?.await()?.toEvents(endedAt, clock, uuidProvider).orEmpty()
            if (events.isNotEmpty()) {
                playbackStatsDao.insertOrIgnore(events)
            }
        }
    }

    private suspend fun findPodcast(episode: BaseEpisode): Podcast? {
        return when (episode) {
            is PodcastEpisode -> podcastDao.findPodcastByUuid(episode.podcastUuid)
            is UserEpisode -> Podcast.userPodcast
        }
    }
}

private class PartialStatsEvent(
    val episode: BaseEpisode,
    val podcast: Podcast,
    val createdAt: Instant,
) {
    fun toEvents(endedAt: Instant, clock: Clock, uuidProvider: UUIDProvider): List<PlaybackStatsEvent> {
        if (endedAt <= createdAt) {
            return emptyList()
        }
        return split(createdAt, endedAt, clock.zone, uuidProvider)
    }

    private tailrec fun split(
        startedAt: Instant,
        endedAt: Instant,
        zone: ZoneId,
        uuidProvider: UUIDProvider,
        events: MutableList<PlaybackStatsEvent> = mutableListOf(),
    ): List<PlaybackStatsEvent> {
        val segmentEnd = minOf(endedAt, startedAt.nextDayStart(zone))
        events += createEvent(uuidProvider.generateUUID(), startedAt, segmentEnd)
        return if (segmentEnd >= endedAt) {
            events
        } else {
            split(segmentEnd, endedAt, zone, uuidProvider, events)
        }
    }

    private fun createEvent(
        uuid: UUID,
        startedAt: Instant,
        endedAt: Instant,
    ) = PlaybackStatsEvent(
        uuid = uuid.toString(),
        episodeUuid = episode.uuid,
        podcastUuid = podcast.uuid,
        episodeTitle = episode.title,
        podcastTitle = podcast.title,
        podcastCategory = podcast.getFirstCategoryUnlocalised(),
        startedAtMs = startedAt.toEpochMilli(),
        durationMs = endedAt.toEpochMilli() - startedAt.toEpochMilli(),
        isSynced = false,
    )

    private fun Instant.nextDayStart(zone: ZoneId): Instant {
        return this.atZone(zone)
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay(zone)
            .toInstant()
    }
}
