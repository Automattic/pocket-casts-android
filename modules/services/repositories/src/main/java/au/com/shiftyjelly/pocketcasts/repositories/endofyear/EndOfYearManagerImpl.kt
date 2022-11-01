package au.com.shiftyjelly.pocketcasts.repositories.endofyear

import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.DateUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import timber.log.Timber
import java.time.DateTimeException
import java.time.LocalDate
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

    override fun findRandomEpisode(): Flow<Episode?> {
        return episodeManager.findRandomEpisode()
    }

    override fun getTotalListeningTimeInSecsForYear(year: Int): Flow<Long?> {
        return try {
            val date = LocalDate.of(year, 1, 1).atStartOfDay()
            val fromEpochTimeInMs = DateUtil.toEpochMillis(date)
            val toEpochTimeInMs = DateUtil.toEpochMillis(date.plusYears(1))

            if (fromEpochTimeInMs != null && toEpochTimeInMs != null) {
                episodeManager.calculateListeningTime(fromEpochTimeInMs, toEpochTimeInMs)
            } else {
                flowOf(null)
            }
        } catch (e: DateTimeException) {
            Timber.e(e)
            flowOf(null)
        }
    }
}
