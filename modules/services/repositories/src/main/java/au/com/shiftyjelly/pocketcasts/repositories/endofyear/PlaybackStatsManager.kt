package au.com.shiftyjelly.pocketcasts.repositories.endofyear

import au.com.shiftyjelly.pocketcasts.models.db.dao.EndOfYearDao
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackPodcastStats
import au.com.shiftyjelly.pocketcasts.utils.extensions.toEpochMillis
import java.time.Clock
import java.time.Instant
import java.time.Year
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Listening facts for Playback. Uses the same "listened in year" definition as the End of Year
 * stats: an episode counts for the year of its last playback interaction.
 */
class PlaybackStatsManager @Inject constructor(
    private val endOfYearDao: EndOfYearDao,
    private val clock: Clock,
) {
    suspend fun getStats(year: Year): PlaybackStats = coroutineScope {
        val thisYearStart = year.toEpochMillis(clock.zone)
        val thisYearEnd = year.plusYears(1).toEpochMillis(clock.zone)
        val lastYearStart = year.minusYears(1).toEpochMillis(clock.zone)

        val podcasts = async { endOfYearDao.getPlaybackPodcastStats(thisYearStart, thisYearEnd) }
        val listeningTimes = async { endOfYearDao.getPlaybackListeningTimes(thisYearStart, thisYearEnd) }
        val completion = async { endOfYearDao.getPlaybackCompletion(thisYearStart, thisYearEnd) }
        val lastYearSeconds = async { endOfYearDao.getTotalPlaybackTime(lastYearStart, thisYearStart) }

        val monthlySeconds = DoubleArray(12)
        listeningTimes.await().forEach { time ->
            val month = Instant.ofEpochMilli(time.interactionEpochMs).atZone(clock.zone).monthValue
            monthlySeconds[month - 1] += time.playedSeconds
        }

        PlaybackStats(
            podcasts = podcasts.await(),
            monthlySeconds = monthlySeconds.toList(),
            lastYearSeconds = lastYearSeconds.await(),
            startedEpisodeCount = completion.await().started,
            completedEpisodeCount = completion.await().completed,
        )
    }
}

data class PlaybackStats(
    /** Per-podcast listening, most listened first. */
    val podcasts: List<PlaybackPodcastStats>,
    /** Listening seconds per month of the year, January first. Always 12 entries. */
    val monthlySeconds: List<Double>,
    val lastYearSeconds: Double,
    /** Distinct episodes listened to in the year. */
    val startedEpisodeCount: Int,
    /** Episodes listened to in the year that were finished (marked played or 90% in). */
    val completedEpisodeCount: Int,
)
