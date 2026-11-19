package au.com.shiftyjelly.pocketcasts.repositories.endofyear

import au.com.shiftyjelly.pocketcasts.models.db.dao.EndOfYearDao
import au.com.shiftyjelly.pocketcasts.models.to.LongestEpisode
import au.com.shiftyjelly.pocketcasts.models.to.RatingStats
import au.com.shiftyjelly.pocketcasts.models.to.TopPodcast
import au.com.shiftyjelly.pocketcasts.utils.extensions.toEpochMillis
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.time.Clock
import java.time.Instant
import java.time.Year
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class EndOfYearManagerImpl @Inject constructor(
    private val endOfYearDao: EndOfYearDao,
    private val clock: Clock,
) : EndOfYearManager {
    override suspend fun isEligibleForEndOfYear(year: Year): Boolean {
        val epochStart = year.toEpochMillis(clock.zone)
        val epochEnd = year.plusYears(1).toEpochMillis(clock.zone)
        return isFeatureEnabled(year) && getTotalPlaybackTime(epochStart, epochEnd) > 5.minutes
    }

    private fun isFeatureEnabled(year: Year) = when (year.value) {
        2025 -> FeatureFlag.isEnabled(Feature.END_OF_YEAR_2025)
        2026 -> FeatureFlag.isEnabled(Feature.PLAYBACK_2026)
        else -> false
    }

    override suspend fun getStats(year: Year): EndOfYearStats = coroutineScope {
        val thisYearStart = year.toEpochMillis(clock.zone)
        val thisYearEnd = year.plusYears(1).toEpochMillis(clock.zone)
        val lastYearStart = year.minusYears(1).toEpochMillis(clock.zone)

        val playedPodcastIds = async { getPlayedPodcastsIds(thisYearStart, thisYearEnd) }
        val playedEpisodeCount = async { getPlayedEpisodeCount(thisYearStart, thisYearEnd) }
        val completedEpisodeCount = async { getCompletedEpisodeCount(thisYearStart, thisYearEnd) }
        val thisYearPlaybackTime = async { getTotalPlaybackTime(thisYearStart, thisYearEnd) }
        val lastYearPlaybackTime = async { getTotalPlaybackTime(lastYearStart, thisYearStart) }
        val topPodcasts = async { getTopPodcasts(thisYearStart, thisYearEnd) }
        val longestPlayedEpisode = async { getLongestPlayedEpisode(thisYearStart, thisYearEnd) }
        val ratingStats = async { getRatingStats(thisYearStart, thisYearEnd) }

        EndOfYearStats(
            playedEpisodeCount = playedEpisodeCount.await(),
            completedEpisodeCount = completedEpisodeCount.await(),
            playedPodcastIds = playedPodcastIds.await(),
            playbackTime = thisYearPlaybackTime.await(),
            lastYearPlaybackTime = lastYearPlaybackTime.await(),
            topPodcasts = topPodcasts.await(),
            longestEpisode = longestPlayedEpisode.await(),
            ratingStats = ratingStats.await(),
        )
    }

    override suspend fun getPlaybackStats(year: Year): PlaybackStats = coroutineScope {
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
        val (started, completed) = completion.await()

        PlaybackStats(
            podcasts = podcasts.await(),
            monthlySeconds = monthlySeconds.asList(),
            lastYearSeconds = lastYearSeconds.await(),
            startedEpisodeCount = started,
            completedEpisodeCount = completed,
        )
    }

    override suspend fun getPlayedEpisodeCount(year: Year): Int {
        return getPlayedEpisodeCount(year.toEpochMillis(clock.zone), year.plusYears(1).toEpochMillis(clock.zone))
    }

    private suspend fun getPlayedEpisodeCount(from: Long, to: Long): Int {
        return endOfYearDao.getPlayedEpisodeCount(from, to)
    }

    private suspend fun getPlayedPodcastsIds(from: Long, to: Long): List<String> {
        return endOfYearDao.getPlayedPodcastIds(from, to)
    }

    private suspend fun getCompletedEpisodeCount(from: Long, to: Long): Int {
        return endOfYearDao.getCompletedEpisodeCount(from, to)
    }

    private suspend fun getTotalPlaybackTime(from: Long, to: Long): Duration {
        return endOfYearDao.getTotalPlaybackTime(from, to).seconds
    }

    private suspend fun getTopPodcasts(from: Long, to: Long): List<TopPodcast> {
        return endOfYearDao.getTopPodcasts(from, to, limit = 5)
    }

    private suspend fun getLongestPlayedEpisode(from: Long, to: Long): LongestEpisode? {
        return endOfYearDao.getLongestPlayedEpisode(from, to)
    }

    private suspend fun getRatingStats(from: Long, to: Long): RatingStats {
        return endOfYearDao.getRatingStats(from, to)
    }
}
