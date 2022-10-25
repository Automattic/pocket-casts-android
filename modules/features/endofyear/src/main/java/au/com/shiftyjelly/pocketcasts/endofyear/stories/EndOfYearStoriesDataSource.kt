package au.com.shiftyjelly.pocketcasts.endofyear.stories

import au.com.shiftyjelly.pocketcasts.endofyear.StoriesDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import timber.log.Timber
import javax.inject.Inject

class EndOfYearStoriesDataSource @Inject constructor() : StoriesDataSource {
    override var stories: List<Story> = emptyList()

    override suspend fun loadStories(): Flow<List<Story>> {
        stories = listOf(StoryFake1(), StoryFake2())
        return flowOf(stories)
    }

    override fun storyAt(index: Int) = try {
        stories[index]
    } catch (e: IndexOutOfBoundsException) {
        Timber.e("Story index is out of bounds")
        null
    }
}
