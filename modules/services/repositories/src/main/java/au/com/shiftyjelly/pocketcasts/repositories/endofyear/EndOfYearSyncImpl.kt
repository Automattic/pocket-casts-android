package au.com.shiftyjelly.pocketcasts.repositories.endofyear

import au.com.shiftyjelly.pocketcasts.models.entity.UserPodcastRating
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.HistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.ratings.RatingsManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.extensions.toDate
import au.com.shiftyjelly.pocketcasts.utils.coroutines.CachedAction
import au.com.shiftyjelly.pocketcasts.utils.coroutines.run
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.Year
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

interface EndOfYearSync {
    suspend fun sync(year: Year = EndOfYearManager.YEAR_TO_SYNC): Boolean
    suspend fun reset()
}

@Singleton
class EndOfYearSyncImpl @Inject constructor(
    private val syncManager: SyncManager,
    private val historyManager: HistoryManager,
    private val ratingsManager: RatingsManager,
    private val endOfYearManager: EndOfYearManager,
    private val settings: Settings,
    private val clock: Clock,
) : EndOfYearSync {
    private val ratingsSync = CachedAction<Unit, Unit> { syncRatings() }
    private val thisYearSync = CachedAction<Year, Unit> { syncHistoryForYear(it) }
    private val lastYearSync = CachedAction<Year, Unit> { syncHistoryForYear(it) }
    private val timestampSetting = settings.lastEoySyncTimestamp

    override suspend fun sync(year: Year): Boolean {
        return when {
            isDataSynced() -> true
            canSyncData() -> supervisorScope {
                val jobs = listOf(
                    thisYearSync.run(year, scope = this),
                    lastYearSync.run(year.minusYears(1), scope = this),
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

    override suspend fun reset() {
        ratingsSync.reset()
        thisYearSync.reset()
        lastYearSync.reset()
        timestampSetting.set(Instant.EPOCH, updateModifiedAt = false)
    }

    private fun isDataSynced(): Boolean {
        return Duration.between(timestampSetting.value, clock.instant()) < Duration.ofDays(1)
    }

    private fun canSyncData(): Boolean {
        return syncManager.isLoggedIn()
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

    private suspend fun syncHistoryForYear(year: Year) {
        // only download the count to check if we are missing history episodes
        val serverCount = syncManager.historyYear(year = year.value, count = true).count
        if (serverCount == null) {
            throw RuntimeException("Failed to get history episode count for year $year")
        }
        val localCount = endOfYearManager.getPlayedEpisodeCount(year)

        if (serverCount > localCount) {
            val history = syncManager.historyYear(year = year.value, count = false).history
            if (history == null) {
                throw RuntimeException("Failed to get history for year $year")
            }
            historyManager.processServerResponse(response = history, updateServerModified = false)
        }
    }
}
