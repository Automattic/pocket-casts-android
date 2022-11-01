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
            endOfYearManager.findRandomPodcasts(),
            endOfYearManager.findRandomEpisode()
        ) { podcasts, episode ->
            val stories = mutableListOf<Story>()

            if (podcasts.isNotEmpty()) stories.add(StoryFake1(podcasts))
            episode?.let { stories.add(StoryFake2(it)) }

            stories
        }
    }
}
