package au.com.shiftyjelly.pocketcasts.repositories.endofyear

import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedNumbers
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.utils.DateUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import timber.log.Timber
import java.time.DateTimeException
import java.time.LocalDate
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class EndOfYearManagerImpl @Inject constructor(
    private val episodeManager: EpisodeManager,
) : EndOfYearManager, CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

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

    override fun findTopPodcastsForYear(year: Int, limit: Int) =
        getYearStartAndEndEpochMs(year)?.let {
            episodeManager.findTopPodcasts(it.start, it.end, limit)
        } ?: flowOf(emptyList())

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
