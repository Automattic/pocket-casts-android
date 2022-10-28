package au.com.shiftyjelly.pocketcasts.endofyear.stories

import au.com.shiftyjelly.pocketcasts.endofyear.StoriesDataSource
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EndOfYearManager
import kotlinx.coroutines.flow.combine
import timber.log.Timber
import javax.inject.Inject

class EndOfYearStoriesDataSource @Inject constructor(
    private val endOfYearManager: EndOfYearManager,
) : StoriesDataSource() {
    override val stories = mutableListOf<Story>()

    override suspend fun loadStories() =
        combine(
            endOfYearManager.findRandomPodcasts(),
            endOfYearManager.findRandomEpisode()
        ) { podcasts, episode ->
            if (podcasts.isNotEmpty()) stories.add(StoryFake1(podcasts))
            episode?.let { stories.add(StoryFake2(it)) }

            stories
        }

    override fun storyAt(index: Int) = try {
        stories[index]
    } catch (e: IndexOutOfBoundsException) {
        Timber.e("Story index is out of bounds")
        null
    }
}
