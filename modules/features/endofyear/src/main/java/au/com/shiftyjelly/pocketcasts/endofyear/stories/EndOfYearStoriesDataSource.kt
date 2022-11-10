package au.com.shiftyjelly.pocketcasts.endofyear.stories

import au.com.shiftyjelly.pocketcasts.endofyear.BuildConfig
import au.com.shiftyjelly.pocketcasts.endofyear.StoriesDataSource
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.transform
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class EndOfYearStoriesDataSource @Inject constructor(
    private val endOfYearManager: EndOfYearManager,
) : StoriesDataSource {
    override fun isEligibleForStories(): Flow<Boolean> =
        endOfYearManager.hasEpisodesPlayedUpto(YEAR, TimeUnit.MINUTES.toSeconds(EPISODE_MINIMUM_PLAYED_TIME_IN_MIN))
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
            endOfYearManager.hasEpisodeInteractedBefore(YEAR),
            endOfYearManager.hasListeningHistoryEpisodesInLimitForYear(YEAR, limit = LISTENING_HISTORY_LIMIT),
        ) { hasEpisodeInteractedBefore, hasListeningHistoryEpisodesInLimitForYear ->
            hasEpisodeInteractedBefore || hasListeningHistoryEpisodesInLimitForYear
        }

    // TODO: Update with the correct endpoint to sync history
    override suspend fun syncListeningHistory() {}

    override fun loadStories(): Flow<List<Story>> {
        return combine(
            endOfYearManager.getTotalListeningTimeInSecsForYear(YEAR),
            endOfYearManager.findListenedCategoriesForYear(YEAR),
            endOfYearManager.findListenedNumbersForYear(YEAR),
            endOfYearManager.findTopPodcastsForYear(YEAR, limit = 10),
            endOfYearManager.findLongestPlayedEpisodeForYear(YEAR)
        ) { listeningTime, listenedCategories, listenedNumbers, topPodcasts, longestEpisode ->
            val stories = mutableListOf<Story>()

            stories.add(StoryIntro())
            listeningTime?.let { stories.add(StoryListeningTime(it, topPodcasts.takeLast(3))) }
            if (listenedCategories.isNotEmpty()) {
                stories.add(StoryListenedCategories(listenedCategories))
                stories.add(StoryTopListenedCategories(listenedCategories))
            }
            if (listenedNumbers.numberOfEpisodes > 1 && listenedNumbers.numberOfPodcasts > 1) {
                stories.add(StoryListenedNumbers(listenedNumbers))
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

    companion object {
        private const val YEAR = 2022
        private const val EPISODE_MINIMUM_PLAYED_TIME_IN_MIN = 30L
        private const val LISTENING_HISTORY_LIMIT = 100
    }
}
