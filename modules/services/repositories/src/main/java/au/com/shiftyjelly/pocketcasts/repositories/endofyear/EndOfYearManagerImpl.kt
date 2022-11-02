package au.com.shiftyjelly.pocketcasts.repositories.endofyear

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class EndOfYearManagerImpl @Inject constructor(
    private val podcastManager: PodcastManager,
    private val episodeManager: EpisodeManager,
) : EndOfYearManager, CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override fun findRandomPodcasts(): Flow<List<Podcast>> {
        return podcastManager.findRandomPodcasts()
    }

    override fun getTotalListeningTimeInSecsForYear(year: Int) =
        getYearStartAndEndEpochMs(year)?.let {
            episodeManager.calculateListeningTime(it.start, it.end)
        } ?: flowOf(null)

    override fun findListeningCategoriesForYear(year: Int) =
        getYearStartAndEndEpochMs(year)?.let {
            episodeManager.findListenedCategories(it.start, it.end)
        } ?: flowOf(emptyList())
}
