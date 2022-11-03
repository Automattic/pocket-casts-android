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
        ) { listeningTime, listenedCategories, listenedNumbers, topFivePodcasts ->
            val stories = mutableListOf<Story>()

            listeningTime?.let { stories.add(StoryListeningTime(it)) }
            if (listenedCategories.isNotEmpty()) {
                stories.add(StoryListenedCategories(listenedCategories))
                stories.add(StoryTopListenedCategories(listenedCategories))
            }
            stories.add(StoryListenedNumbers(listenedNumbers))
            if (topFivePodcasts.isNotEmpty()) {
                stories.add(StoryTopPodcast(topFivePodcasts.first()))
                stories.add(StoryTopFivePodcasts(topFivePodcasts))
            }

            stories
        }
    }

    companion object {
        private const val YEAR = 2022
    }
}
