package au.com.shiftyjelly.pocketcasts.endofyear

import au.com.shiftyjelly.pocketcasts.endofyear.stories.Story
import kotlinx.coroutines.flow.Flow

interface StoriesDataSource {
    var stories: List<Story>
    val totalLengthInMs
        get() = stories.sumOf { it.storyLength }

    suspend fun loadStories(): Flow<List<Story>>
    fun storyAt(index: Int): Story?
}
