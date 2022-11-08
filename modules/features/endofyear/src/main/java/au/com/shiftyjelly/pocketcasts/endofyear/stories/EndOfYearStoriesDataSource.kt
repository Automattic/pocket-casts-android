package au.com.shiftyjelly.pocketcasts.endofyear.stories

import au.com.shiftyjelly.pocketcasts.endofyear.StoriesDataSource
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class EndOfYearStoriesDataSource @Inject constructor(
    private val endOfYearManager: EndOfYearManager,
) : StoriesDataSource {
    override suspend fun loadStories(): Flow<List<Story>> {
        return combine(
            endOfYearManager.getTotalListeningTimeInSecsForYear(YEAR),
            endOfYearManager.findListenedCategoriesForYear(YEAR),
            endOfYearManager.findListenedNumbersForYear(YEAR),
            endOfYearManager.findTopPodcastsForYear(YEAR, limit = 5),
            endOfYearManager.findLongestPlayedEpisodeForYear(YEAR)
        ) { listeningTime, listenedCategories, listenedNumbers, topFivePodcasts, longestEpisode ->
            val stories = mutableListOf<Story>()

            stories.add(StoryIntro())
            listeningTime?.let { stories.add(StoryListeningTime(it)) }
            if (listenedCategories.isNotEmpty()) {
                stories.add(StoryListenedCategories(listenedCategories))
                stories.add(StoryTopListenedCategories(listenedCategories))
            }
            if (listenedNumbers.numberOfEpisodes > 1 && listenedNumbers.numberOfPodcasts > 1) {
                stories.add(StoryListenedNumbers(listenedNumbers))
            }
            if (topFivePodcasts.isNotEmpty()) {
                stories.add(StoryTopPodcast(topFivePodcasts.first()))
                if (topFivePodcasts.size > 1) {
                    stories.add(StoryTopFivePodcasts(topFivePodcasts))
                }
            }
            longestEpisode?.let { stories.add(StoryLongestEpisode(it)) }
            stories.add(StoryEpilogue())

            stories
        }
    }

    companion object {
        private const val YEAR = 2022
    }
}
