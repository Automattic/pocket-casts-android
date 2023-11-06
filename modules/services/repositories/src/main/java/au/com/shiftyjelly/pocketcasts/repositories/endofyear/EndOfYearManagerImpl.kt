package au.com.shiftyjelly.pocketcasts.repositories.endofyear

import au.com.shiftyjelly.pocketcasts.models.db.helper.EpisodesStartedAndCompleted
import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedCategory
import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedNumbers
import au.com.shiftyjelly.pocketcasts.models.db.helper.LongestEpisode
import au.com.shiftyjelly.pocketcasts.models.db.helper.TopPodcast
import au.com.shiftyjelly.pocketcasts.models.db.helper.YearOverYearListeningTime
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.Story
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryCompletionRate
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryEpilogue
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryIntro
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryListenedNumbers
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryListeningTime
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryLongestEpisode
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryTopFivePodcasts
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryTopListenedCategories
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryTopPodcast
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryYearOverYear
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.HistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.math.min

class EndOfYearManagerImpl @Inject constructor(
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
    private val historyManager: HistoryManager,
    private val syncManager: SyncManager,
    private val settings: Settings,
) : EndOfYearManager, CoroutineScope {

    companion object {
        private const val YEAR = 2023
        private const val EPISODE_MINIMUM_PLAYED_TIME_IN_MIN = 5L
    }

    private fun yearStart(year: Int) = epochAtStartOfYear(year)
    private fun yearEnd(year: Int) = epochAtStartOfYear(year + 1)

    private val yearsToSync
        get() = if (settings.userTier != UserTier.Free) {
            listOf(YEAR, YEAR - 1)
        } else {
            listOf(YEAR)
        }

    private fun epochAtStartOfYear(year: Int) = LocalDate.of(year, 1, 1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override suspend fun isEligibleForStories(): Boolean =
        hasEpisodesPlayedUpto(YEAR, TimeUnit.MINUTES.toSeconds(EPISODE_MINIMUM_PLAYED_TIME_IN_MIN)) &&
            FeatureFlag.isEnabled(Feature.END_OF_YEAR_ENABLED)

    private var synced: Double = 0.0
    private var syncTotal: Double = 0.0

    override suspend fun downloadListeningHistory(onProgressChanged: (Float) -> Unit) {
        if (!syncManager.isLoggedIn()) {
            return
        }
        resetSyncCount()
        coroutineScope {
            awaitAll(
                *yearsToSync.map { year ->
                    // Download listening history for each year in parallel
                    async { downloadListeningHistory(year, onProgressChanged) }
                }.toTypedArray()
            )
        }
        onProgressChanged(1f)
    }

    /**
     * Download the year's listening history.
     */
    private suspend fun downloadListeningHistory(year: Int, onProgressChanged: (Float) -> Unit) {
        // check for an episode interacted with before this year and assume they have the full listening history if they exist
        if (anyEpisodeInteractionBeforeYear(year)) {
            return
        }
        // only download the count to check if we are missing history episodes
        val countResponse = syncManager.historyYear(year = year, count = true)
        onProgressChanged(0.1f)
        val serverCount = countResponse.count ?: 0
        val localCount = countEpisodeInteractionsInYear(year)
        Timber.i("End of Year: Server listening history. server: ${countResponse.count} local: $localCount")
        if (serverCount > localCount) {
            // sync the year's listening history
            val response = syncManager.historyYear(year = year, count = false)
            onProgressChanged(0.2f)
            val history = response.history ?: return
            historyManager.processServerResponse(
                response = history,
                updateServerModified = false,
                updateSyncTotal = { syncTotal += it },
                onProgressChanged = {
                    synced += 1
                    val progress = min((0.2f + (synced / syncTotal) * 0.8f), 0.95).toFloat()
                    onProgressChanged(progress)
                },
            )
        }
    }

    private fun resetSyncCount() {
        synced = 0.0
        syncTotal = 0.0
    }

    override suspend fun loadStories(): List<Story> {
        val listeningTime = getTotalListeningTimeInSecsForYear(YEAR)
        val listenedCategories = findListenedCategoriesForYear(YEAR)
        val listenedNumbers = findListenedNumbersForYear(YEAR)
        val topPodcasts = findTopPodcastsForYear(YEAR, limit = 10)
        val longestEpisode = findLongestPlayedEpisodeForYear(YEAR)
        val yearOverYearListeningTime = getYearOverYearListeningTime(YEAR)
        val episodesStartedAndCompleted = countEpisodesStartedAndCompleted(YEAR)
        val stories = mutableListOf<Story>()

        stories.add(StoryIntro())
        if (listenedNumbers.numberOfEpisodes > 1 && listenedNumbers.numberOfPodcasts > 1) {
            stories.add(StoryListenedNumbers(listenedNumbers, topPodcasts))
        }
        if (topPodcasts.isNotEmpty()) {
            stories.add(StoryTopPodcast(topPodcasts = topPodcasts))
            if (topPodcasts.size > 1) {
                stories.add(StoryTopFivePodcasts(topPodcasts.take(5)))
            }
        }
        if (listenedCategories.isNotEmpty()) {
            stories.add(StoryTopListenedCategories(listenedCategories))
        }
        listeningTime?.let { stories.add(StoryListeningTime(it)) }
        longestEpisode?.let { stories.add(StoryLongestEpisode(it)) }
        if (yearOverYearListeningTime.totalPlayedTimeThisYear != 0L || yearOverYearListeningTime.totalPlayedTimeLastYear != 0L) {
            stories.add(StoryYearOverYear(yearOverYearListeningTime))
        }
        stories.add(StoryCompletionRate(episodesStartedAndCompleted))
        stories.add(StoryEpilogue())

        return stories
    }

    /* Returns whether user listened to at least one episode for more than given time for the year */
    override suspend fun hasEpisodesPlayedUpto(year: Int, playedUpToInSecs: Long): Boolean {
        return episodeManager.countEpisodesPlayedUpto(yearStart(year), yearEnd(year), playedUpToInSecs) > 0
    }

    private suspend fun anyEpisodeInteractionBeforeYear(year: Int): Boolean {
        return episodeManager.findEpisodeInteractedBefore(yearStart(year)) != null
    }

    private suspend fun countEpisodeInteractionsInYear(year: Int): Int {
        return episodeManager.countEpisodesInListeningHistory(yearStart(year), yearEnd(year))
    }

    override suspend fun getTotalListeningTimeInSecsForYear(year: Int): Long? {
        return episodeManager.calculateListeningTime(yearStart(year), yearEnd(year))
    }

    override suspend fun findListenedCategoriesForYear(year: Int): List<ListenedCategory> {
        return episodeManager.findListenedCategories(yearStart(year), yearEnd(year))
    }

    override suspend fun findListenedNumbersForYear(year: Int): ListenedNumbers {
        return episodeManager.findListenedNumbers(yearStart(year), yearEnd(year))
    }

    /* Returns top podcasts ordered by total played time. If there's a tie on total played time, check number of played episodes. */
    override suspend fun findTopPodcastsForYear(year: Int, limit: Int): List<TopPodcast> {
        return podcastManager.findTopPodcasts(yearStart(year), yearEnd(year), limit)
    }

    override suspend fun findLongestPlayedEpisodeForYear(year: Int): LongestEpisode? {
        return episodeManager.findLongestPlayedEpisode(yearStart(year), yearEnd(year))
    }

    override suspend fun getYearOverYearListeningTime(thisYear: Int): YearOverYearListeningTime {
        return episodeManager.yearOverYearListeningTime(
            fromEpochMsPreviousYear = yearStart(thisYear - 1),
            toEpochMsPreviousYear = yearEnd(thisYear - 1),
            fromEpochMsCurrentYear = yearStart(thisYear),
            toEpochMsCurrentYear = yearEnd(thisYear),
        )
    }

    override suspend fun countEpisodesStartedAndCompleted(year: Int): EpisodesStartedAndCompleted {
        return episodeManager.countEpisodesStartedAndCompleted(yearStart(year), yearEnd(year))
    }
}
