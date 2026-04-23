package au.com.shiftyjelly.pocketcasts.repositories.stats

import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.models.db.dao.PlaybackStatsDao
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PlaybackStatsEvent
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.utils.UUIDProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface PlaybackStatsCollector {
    fun onStart()
    fun onStop()
}

@Singleton
class PersistentPlaybackStatsCollector @Inject constructor(
    private val queue: UpNextQueue,
    private val playbackStatsDao: PlaybackStatsDao,
    private val clock: Clock,
    private val uuidProvider: UUIDProvider,
    @ApplicationScope private val scope: CoroutineScope,
) : PlaybackStatsCollector {
    private val stateLock = Any()
    private var isPlaying = false
    private var partialEvent: PartialStatsEvent? = null

    override fun onStart() {
        val startedAt = clock.instant()
        val episode = queue.currentEpisode ?: return
        synchronized(stateLock) {
            if (isPlaying) {
                return
            }
            isPlaying = true

            partialEvent = PartialStatsEvent(episode, startedAt)
        }
    }

    override fun onStop() {
        val endedAt = clock.instant()
        val events = synchronized(stateLock) {
            if (!isPlaying) {
                return
            }
            isPlaying = false

            partialEvent?.toEvents(endedAt, clock, uuidProvider).also { partialEvent = null }
        }
        if (!events.isNullOrEmpty()) {
            scope.launch {
                playbackStatsDao.insertOrIgnore(events)
            }
        }
    }
}

private class PartialStatsEvent(
    val episode: BaseEpisode,
    val createdAt: Instant,
) {
    fun toEvents(endedAt: Instant, clock: Clock, uuidProvider: UUIDProvider): List<PlaybackStatsEvent> {
        if (endedAt <= createdAt) {
            return emptyList()
        }
        return split(createdAt, endedAt, clock.zone, uuidProvider)
    }

    /**
     * Splits a session into one event per UTC based calendar day, cutting each
     * segment at the next midnight until the play session ends
     */
    private fun split(
        startedAt: Instant,
        endedAt: Instant,
        zone: ZoneId,
        uuidProvider: UUIDProvider,
    ): List<PlaybackStatsEvent> {
        return buildList {
            var segmentStart = startedAt
            while (true) {
                val segmentEnd = minOf(endedAt, segmentStart.nextDayStart(zone))
                add(createEvent(uuidProvider.generateUUID(), segmentStart, segmentEnd))

                if (segmentEnd >= endedAt) {
                    break
                }
                segmentStart = segmentEnd
            }
        }
    }

    private fun createEvent(
        uuid: UUID,
        startedAt: Instant,
        endedAt: Instant,
    ) = PlaybackStatsEvent(
        uuid = uuid.toString(),
        episodeUuid = episode.uuid,
        podcastUuid = episode.podcastOrSubstituteUuid,
        startedAtMs = startedAt.toEpochMilli(),
        durationMs = endedAt.toEpochMilli() - startedAt.toEpochMilli(),
    )

    private fun Instant.nextDayStart(zone: ZoneId): Instant {
        return this.atZone(zone)
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay(zone)
            .toInstant()
    }
}
