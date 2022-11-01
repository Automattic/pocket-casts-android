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
            endOfYearManager.findRandomPodcasts(),
            endOfYearManager.findRandomEpisode(),
        ) { listeningTime, podcasts, episode, ->
            val stories = mutableListOf<Story>()

            listeningTime?.let { stories.add(StoryListeningTime(it)) }
            if (podcasts.isNotEmpty()) stories.add(StoryFake1(podcasts))
            episode?.let { stories.add(StoryFake2(it)) }

            stories
        }
    }

    companion object {
        private const val YEAR = 2022
    }
}
