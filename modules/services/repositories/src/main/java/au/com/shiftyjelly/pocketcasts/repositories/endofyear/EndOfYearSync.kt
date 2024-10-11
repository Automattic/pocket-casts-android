package au.com.shiftyjelly.pocketcasts.repositories.endofyear

import au.com.shiftyjelly.pocketcasts.models.entity.UserPodcastRating
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.HistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.ratings.RatingsManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.extensions.toDate
import au.com.shiftyjelly.pocketcasts.utils.coroutines.CachedAction
import au.com.shiftyjelly.pocketcasts.utils.coroutines.run
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

@Singleton
class EndOfYearSync @Inject constructor(
    private val syncManager: SyncManager,
    private val historyManager: HistoryManager,
    private val ratingsManager: RatingsManager,
    private val episodeManager: EpisodeManager,
    private val settings: Settings,
    private val clock: Clock,
) {
    private val ratingsSync = CachedAction<Unit, Unit> { syncRatings() }
    private val thisYearSync = CachedAction<Int, Unit> { syncHistoryForYear(it) }
    private val lastYearSync = CachedAction<Int, Unit> { syncHistoryForYear(it) }
    private val timestampSetting = settings.lastEoySyncTimestamp

    suspend fun sync(year: Int = EndOfYearManager.YEAR_TO_SYNC): Boolean {
        return when {
            isDataSynced() -> true
            canSyncData() -> supervisorScope {
                val jobs = listOf(
                    thisYearSync.run(year, scope = this),
                    lastYearSync.run(year - 1, scope = this),
                    ratingsSync.run(scope = this),
                )
                runCatching { jobs.awaitAll() }
                    .map { true }
                    .onSuccess { timestampSetting.set(clock.instant(), updateModifiedAt = false) }
                    .getOrElse { error -> if (error !is CancellationException) false else throw error }
            }
            else -> false
        }
    }

    suspend fun reset() {
        ratingsSync.reset()
        thisYearSync.reset()
        lastYearSync.reset()
        timestampSetting.set(Instant.EPOCH, updateModifiedAt = false)
    }

    private fun isDataSynced(): Boolean {
        return Duration.between(timestampSetting.value, clock.instant()) > Duration.ofHours(6)
    }

    private fun canSyncData(): Boolean {
        return FeatureFlag.isEnabled(Feature.END_OF_YEAR_2024) && syncManager.isLoggedIn()
    }

    private suspend fun syncRatings() {
        val lastSyncTimeString = settings.getLastModified()
        val lastSyncTime = runCatching { Instant.parse(lastSyncTimeString) }.getOrDefault(Instant.EPOCH)
        // Check if we synced account in the last day and assume that ratings are accurate if that's the case
        if (Duration.between(lastSyncTime, clock.instant()) < Duration.ofDays(1)) {
            return
        }

        val ratings = syncManager.getPodcastRatings()?.podcastRatingsList
        if (ratings == null) {
            throw RuntimeException("Failed to sync ratings")
        }
        val userRatings = ratings.mapNotNull { rating ->
            val modifiedAt = rating.modifiedAt.toDate() ?: return@mapNotNull null
            UserPodcastRating(
                podcastUuid = rating.podcastUuid,
                rating = rating.podcastRating,
                modifiedAt = modifiedAt,
            )
        }
        ratingsManager.updateUserRatings(userRatings)
    }

    private suspend fun syncHistoryForYear(year: Int) {
        // only download the count to check if we are missing history episodes
        val serverCount = syncManager.historyYear(year = year, count = true).count
        if (serverCount == null) {
            throw RuntimeException("Failed to get history episode count for year $year")
        }
        val localCount = countEpisodeInteractionsInYear(year)

        if (serverCount > localCount) {
            val history = syncManager.historyYear(year = year, count = false).history
            if (history == null) {
                throw RuntimeException("Failed to get history for year $year")
            }
            historyManager.processServerResponse(response = history, updateServerModified = false)
        }
    }

    private suspend fun countEpisodeInteractionsInYear(year: Int): Int {
        return episodeManager.countEpisodesInListeningHistory(yearStart(year), yearEnd(year))
    }

    private fun yearStart(year: Int) = epochAtStartOfYear(year)

    private fun yearEnd(year: Int) = epochAtStartOfYear(year + 1)

    private fun epochAtStartOfYear(year: Int) = LocalDate.of(year, 1, 1).atStartOfDay().atZone(clock.zone).toInstant().toEpochMilli()
}
