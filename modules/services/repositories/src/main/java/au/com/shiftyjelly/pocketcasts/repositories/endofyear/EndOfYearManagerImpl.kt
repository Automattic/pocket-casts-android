package au.com.shiftyjelly.pocketcasts.repositories.endofyear

import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedCategory
import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedNumbers
import au.com.shiftyjelly.pocketcasts.models.db.helper.LongestEpisode
import au.com.shiftyjelly.pocketcasts.models.db.helper.TopPodcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.BuildConfig
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.Story
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryEpilogue
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryIntro
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryListenedCategories
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryListenedNumbers
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryListeningTime
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryLongestEpisode
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryTopFivePodcasts
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryTopListenedCategories
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryTopPodcast
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.HistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class EndOfYearManagerImpl @Inject constructor(
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
    private val syncServerManager: SyncServerManager,
    private val historyManager: HistoryManager,
    private val settings: Settings,
) : EndOfYearManager, CoroutineScope {

    companion object {
        private const val YEAR = 2022
        private const val EPISODE_MINIMUM_PLAYED_TIME_IN_MIN = 5L
    }

    private val yearStart = epochAtStartOfYear(YEAR)
    private val yearEnd = epochAtStartOfYear(YEAR + 1)

    private fun epochAtStartOfYear(year: Int) = LocalDate.of(year, 1, 1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override suspend fun isEligibleForStories(): Boolean =
        hasEpisodesPlayedUpto(YEAR, TimeUnit.MINUTES.toSeconds(EPISODE_MINIMUM_PLAYED_TIME_IN_MIN)) &&
            BuildConfig.END_OF_YEAR_ENABLED

    /**
     * Download the year's listening history.
     */
    override suspend fun downloadListeningHistory(onProgressChanged: (Float) -> Unit) {
        if (!settings.isLoggedIn()) {
            return
        }
        // check for an episode interacted with before this year and assume they have the full listening history if they exist
        if (anyEpisodeInteractionBeforeYear()) {
            return
        }
        // only download the count to check if we are missing history episodes
        val countResponse = syncServerManager.historyYear(year = YEAR, count = true)
        onProgressChanged(0.1f)
        val serverCount = countResponse.count ?: 0
        val localCount = countEpisodeInteractionsInYear()
        Timber.i("End of Year: Server listening history. server: ${countResponse.count} local: $localCount")
        if (serverCount > localCount) {
            // sync the year's listening history
            val response = syncServerManager.historyYear(year = YEAR, count = false)
            onProgressChanged(0.2f)
            val history = response.history ?: return
            historyManager.processServerResponse(
                response = history,
                updateServerModified = false,
                onProgressChanged = {
                    onProgressChanged(0.2f + (it * 0.8f))
                },
            )
        } else {
            onProgressChanged(1f)
        }
    }

    override suspend fun loadStories(): List<Story> {
        val listeningTime = getTotalListeningTimeInSecsForYear(YEAR)
        val listenedCategories = findListenedCategoriesForYear(YEAR)
        val listenedNumbers = findListenedNumbersForYear(YEAR)
        val topPodcasts = findTopPodcastsForYear(YEAR, limit = 10)
        val longestEpisode = findLongestPlayedEpisodeForYear(YEAR)
        val stories = mutableListOf<Story>()

        stories.add(StoryIntro())
        listeningTime?.let { stories.add(StoryListeningTime(it, topPodcasts.takeLast(3))) }
        if (listenedCategories.isNotEmpty()) {
            stories.add(StoryListenedCategories(listenedCategories))
            stories.add(StoryTopListenedCategories(listenedCategories))
        }
        if (listenedNumbers.numberOfEpisodes > 1 && listenedNumbers.numberOfPodcasts > 1) {
            stories.add(StoryListenedNumbers(listenedNumbers, topPodcasts))
        }
        if (topPodcasts.isNotEmpty()) {
            stories.add(StoryTopPodcast(topPodcasts.first()))
            if (topPodcasts.size > 1) {
                stories.add(StoryTopFivePodcasts(topPodcasts.take(5)))
            }
        }
        longestEpisode?.let { stories.add(StoryLongestEpisode(it)) }
        stories.add(StoryEpilogue())

        return stories
    }

    /* Returns whether user listened to at least one episode for more than given time for the year */
    override suspend fun hasEpisodesPlayedUpto(year: Int, playedUpToInSecs: Long): Boolean {
        return episodeManager.countEpisodesPlayedUpto(yearStart, yearEnd, playedUpToInSecs) > 0
    }

    private suspend fun anyEpisodeInteractionBeforeYear(): Boolean {
        return episodeManager.findEpisodeInteractedBefore(yearStart) != null
    }

    private suspend fun countEpisodeInteractionsInYear(): Int {
        return episodeManager.countEpisodesInListeningHistory(yearStart, yearEnd)
    }

    override suspend fun getTotalListeningTimeInSecsForYear(year: Int): Long? {
        return episodeManager.calculateListeningTime(yearStart, yearEnd)
    }

    override suspend fun findListenedCategoriesForYear(year: Int): List<ListenedCategory> {
        return episodeManager.findListenedCategories(yearStart, yearEnd)
    }

    override suspend fun findListenedNumbersForYear(year: Int): ListenedNumbers {
        return episodeManager.findListenedNumbers(yearStart, yearEnd)
    }

    /* Returns top podcasts ordered by number of played episodes. If there's a tie on number of played episodes,
    played time is checked. */
    override suspend fun findTopPodcastsForYear(year: Int, limit: Int): List<TopPodcast> {
        return podcastManager.findTopPodcasts(yearStart, yearEnd, limit)
    }

    override suspend fun findLongestPlayedEpisodeForYear(year: Int): LongestEpisode? {
        return episodeManager.findLongestPlayedEpisode(yearStart, yearEnd)
    }
}
