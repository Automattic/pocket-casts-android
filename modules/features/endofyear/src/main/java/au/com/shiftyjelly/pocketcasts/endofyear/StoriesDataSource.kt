package au.com.shiftyjelly.pocketcasts.endofyear

import au.com.shiftyjelly.pocketcasts.endofyear.stories.Story

interface StoriesDataSource {
    var stories: List<Story>
    val totalLengthInMs
        get() = stories.sumOf { it.storyLength }

    fun loadStories(): List<Story>
    fun storyAt(index: Int): Story?
}
