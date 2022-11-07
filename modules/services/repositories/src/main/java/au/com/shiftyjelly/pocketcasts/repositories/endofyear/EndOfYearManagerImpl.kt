package au.com.shiftyjelly.pocketcasts.repositories.endofyear

import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedNumbers
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.DateUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.transform
import timber.log.Timber
import java.time.DateTimeException
import java.time.LocalDate
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class EndOfYearManagerImpl @Inject constructor(
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
) : EndOfYearManager, CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    /* Returns whether user listened to at least one episode for more than given time for the year */
    override fun hasEpisodesPlayedUpto(year: Int, playedUpToInSecs: Long): Flow<Boolean> =
        getYearStartAndEndEpochMs(year)?.let {
            episodeManager.countEpisodesPlayedUpto(it.start, it.end, playedUpToInSecs).transform { count -> emit(count > 0) }
        } ?: flowOf(false)

    override fun getTotalListeningTimeInSecsForYear(year: Int) =
        getYearStartAndEndEpochMs(year)?.let {
            episodeManager.calculateListeningTime(it.start, it.end)
        } ?: flowOf(null)

    override fun findListenedCategoriesForYear(year: Int) =
        getYearStartAndEndEpochMs(year)?.let {
            episodeManager.findListenedCategories(it.start, it.end)
        } ?: flowOf(emptyList())

    override fun findListenedNumbersForYear(year: Int) =
        getYearStartAndEndEpochMs(year)?.let {
            episodeManager.findListenedNumbers(it.start, it.end)
        } ?: flowOf(ListenedNumbers())

    /* Returns top podcasts ordered by number of played episodes. If there's a tie on number of played episodes,
    played time is checked. */
    override fun findTopPodcastsForYear(year: Int, limit: Int) =
        getYearStartAndEndEpochMs(year)?.let {
            podcastManager.findTopPodcasts(it.start, it.end, limit)
        } ?: flowOf(emptyList())

    override fun findLongestPlayedEpisodeForYear(year: Int) =
        getYearStartAndEndEpochMs(year)?.let {
            episodeManager.findLongestPlayedEpisode(it.start, it.end)
        } ?: flowOf(null)

    private fun getYearStartAndEndEpochMs(year: Int): YearStartAndEndEpochMs? {
        var yearStartAndEndEpochMs: YearStartAndEndEpochMs? = null
        try {
            val date = LocalDate.of(year, 1, 1).atStartOfDay()
            val fromEpochTimeInMs = DateUtil.toEpochMillis(date)
            val toEpochTimeInMs = DateUtil.toEpochMillis(date.plusYears(1))
            if (fromEpochTimeInMs != null && toEpochTimeInMs != null) {
                yearStartAndEndEpochMs = YearStartAndEndEpochMs(fromEpochTimeInMs, toEpochTimeInMs)
            }
        } catch (e: DateTimeException) {
            Timber.e(e)
        }
        return yearStartAndEndEpochMs
    }

    data class YearStartAndEndEpochMs(val start: Long, val end: Long)
}
