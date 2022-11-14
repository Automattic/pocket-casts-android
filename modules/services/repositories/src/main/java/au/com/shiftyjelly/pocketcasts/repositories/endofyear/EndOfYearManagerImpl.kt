package au.com.shiftyjelly.pocketcasts.repositories.endofyear

import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedNumbers
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
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.DateUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.transform
import timber.log.Timber
import java.time.DateTimeException
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class EndOfYearManagerImpl @Inject constructor(
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
) : EndOfYearManager, CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override fun isEligibleForStories(): Flow<Boolean> =
        hasEpisodesPlayedUpto(YEAR, TimeUnit.MINUTES.toSeconds(EPISODE_MINIMUM_PLAYED_TIME_IN_MIN))
            .transform { emit(it && BuildConfig.END_OF_YEAR_ENABLED) }

    /* Check if the user has the full listening history or not.
     This is not 100% accurate. In order to determine if the user
     has the full history we check for their latest episode listened.
     If this episode was interacted in 2021 or before, we assume they
     have the full history.
     If this is not true, we check for the total number of items of
     this year. If the number is less than or equal 100, we assume they
     have the full history. */
    override fun hasFullListeningHistory(): Flow<Boolean> =
        combine(
            hasEpisodeInteractedBefore(YEAR),
            hasListeningHistoryEpisodesInLimitForYear(YEAR, limit = LISTENING_HISTORY_LIMIT),
        ) { hasEpisodeInteractedBefore, hasListeningHistoryEpisodesInLimitForYear ->
            hasEpisodeInteractedBefore || hasListeningHistoryEpisodesInLimitForYear
        }

    override fun loadStories(): Flow<List<Story>> {
        return combine(
            getTotalListeningTimeInSecsForYear(YEAR),
            findListenedCategoriesForYear(YEAR),
            findListenedNumbersForYear(YEAR),
            findTopPodcastsForYear(YEAR, limit = 10),
            findLongestPlayedEpisodeForYear(YEAR)
        ) { listeningTime, listenedCategories, listenedNumbers, topPodcasts, longestEpisode ->
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

            stories
        }
    }

    /* Returns whether user listened to at least one episode for more than given time for the year */
    override fun hasEpisodesPlayedUpto(year: Int, playedUpToInSecs: Long): Flow<Boolean> =
        getYearStartAndEndEpochMs(year)?.let {
            episodeManager.countEpisodesPlayedUpto(it.start, it.end, playedUpToInSecs).transform { count -> emit(count > 0) }
        } ?: flowOf(false)

    override fun hasEpisodeInteractedBefore(year: Int): Flow<Boolean> =
        getYearStartAndEndEpochMs(year)?.let {
            episodeManager.findEpisodeInteractedBefore(it.start).transform { episode -> emit(episode != null) }
        } ?: flowOf(false)

    override fun hasListeningHistoryEpisodesInLimitForYear(year: Int, limit: Int): Flow<Boolean> =
        getYearStartAndEndEpochMs(year)?.let {
            episodeManager.countEpisodesInListeningHistory(it.start, it.end).transform { count -> emit(count <= limit) }
        } ?: flowOf(false)

    override fun getTotalListeningTimeInSecsForYear(year: Int) =
        getYearStartAndEndEpochMs(year)?.let {
            episodeManager.calculateListeningTime(it.start, it.end)
        } ?: flowOf(null)

    override fun findListenedCategoriesForYear(year: Int) =
        getYearStartAndEndEpochMs(year)?.let {
            episodeManager.findListenedCategories(it.start, it.end)
        } ?: flowOf(emptyList())

    override fun findListenedNumbersForYear(year: Int) =
        getYearStartAndEndEpochMs(year)?.let {
            episodeManager.findListenedNumbers(it.start, it.end)
        } ?: flowOf(ListenedNumbers())

    /* Returns top podcasts ordered by number of played episodes. If there's a tie on number of played episodes,
    played time is checked. */
    override fun findTopPodcastsForYear(year: Int, limit: Int) =
        getYearStartAndEndEpochMs(year)?.let {
            podcastManager.findTopPodcasts(it.start, it.end, limit)
        } ?: flowOf(emptyList())

    override fun findLongestPlayedEpisodeForYear(year: Int) =
        getYearStartAndEndEpochMs(year)?.let {
            episodeManager.findLongestPlayedEpisode(it.start, it.end)
        } ?: flowOf(null)

    private fun getYearStartAndEndEpochMs(year: Int): YearStartAndEndEpochMs? {
        var yearStartAndEndEpochMs: YearStartAndEndEpochMs? = null
        try {
            val date = LocalDate.of(year, 1, 1).atStartOfDay()
            val fromEpochTimeInMs = DateUtil.toEpochMillis(date)
            val toEpochTimeInMs = DateUtil.toEpochMillis(date.plusYears(1))
            if (fromEpochTimeInMs != null && toEpochTimeInMs != null) {
                yearStartAndEndEpochMs = YearStartAndEndEpochMs(fromEpochTimeInMs, toEpochTimeInMs)
            }
        } catch (e: DateTimeException) {
            Timber.e(e)
        }
        return yearStartAndEndEpochMs
    }

    data class YearStartAndEndEpochMs(val start: Long, val end: Long)

    companion object {
        private const val YEAR = 2022
        private const val EPISODE_MINIMUM_PLAYED_TIME_IN_MIN = 30L
        private const val LISTENING_HISTORY_LIMIT = 100
    }
}
