package au.com.shiftyjelly.pocketcasts.endofyear.stories

import au.com.shiftyjelly.pocketcasts.endofyear.StoriesDataSource
import timber.log.Timber
import javax.inject.Inject

class EndOfYearStoriesDataSource @Inject constructor() : StoriesDataSource {
    override var stories: List<Story> = emptyList()

    override fun loadStories(): List<Story> {
        stories = listOf(StoryFake1(), StoryFake2())
        return stories
    }

    override fun storyAt(index: Int) = try {
        stories[index]
    } catch (e: IndexOutOfBoundsException) {
        Timber.e("Story index is out of bounds")
        null
    }
}
