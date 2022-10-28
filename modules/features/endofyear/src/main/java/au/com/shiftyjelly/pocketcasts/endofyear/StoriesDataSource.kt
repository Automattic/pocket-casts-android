package au.com.shiftyjelly.pocketcasts.endofyear

import au.com.shiftyjelly.pocketcasts.endofyear.stories.Story
import kotlinx.coroutines.flow.Flow

abstract class StoriesDataSource {
    protected abstract val stories: List<Story>

    val numOfStories: Int
        get() = stories.size
    val totalLengthInMs
        get() = storyLengthsInMs.sum() + gapLengthsInMs
    val storyLengthsInMs: List<Long>
        get() = stories.map { it.storyLength }
    private val gapLengthsInMs: Long
        get() = STORY_GAP_LENGTH_MS * numOfStories.minus(1).coerceAtLeast(0)

    abstract suspend fun loadStories(): Flow<List<Story>>
    abstract fun storyAt(index: Int): Story?

    companion object {
        const val STORY_GAP_LENGTH_MS = 100L
    }
}
