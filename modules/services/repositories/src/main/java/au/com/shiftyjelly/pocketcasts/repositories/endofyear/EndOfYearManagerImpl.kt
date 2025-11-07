package au.com.shiftyjelly.pocketcasts.repositories.endofyear

import au.com.shiftyjelly.pocketcasts.models.db.dao.EndOfYearDao
import au.com.shiftyjelly.pocketcasts.models.to.LongestEpisode
import au.com.shiftyjelly.pocketcasts.models.to.RatingStats
import au.com.shiftyjelly.pocketcasts.models.to.TopPodcast
import au.com.shiftyjelly.pocketcasts.utils.extensions.toEpochMillis
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.time.Clock
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
        return FeatureFlag.isEnabled(Feature.END_OF_YEAR_2025) && getTotalPlaybackTime(epochStart, epochEnd) > 5.minutes
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
