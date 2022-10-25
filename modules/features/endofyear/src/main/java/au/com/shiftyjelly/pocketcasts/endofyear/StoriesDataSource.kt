package au.com.shiftyjelly.pocketcasts.endofyear

import au.com.shiftyjelly.pocketcasts.endofyear.stories.Story
import kotlinx.coroutines.flow.Flow

abstract class StoriesDataSource {
    protected abstract val stories: List<Story>

    val numOfStories: Int
        get() = stories.size
    val totalLengthInMs
        get() = stories.sumOf { it.storyLength }

    abstract suspend fun loadStories(): Flow<List<Story>>
    abstract fun storyAt(index: Int): Story?
}
