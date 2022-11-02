package au.com.shiftyjelly.pocketcasts.repositories.endofyear

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.utils.DateUtil
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.time.DateTimeException
import java.time.LocalDate

interface EndOfYearManager {
    fun findRandomPodcasts(): Flow<List<Podcast>>
    fun getTotalListeningTimeInSecsForYear(year: Int): Flow<Long?>

    fun getYearStartAndEndEpochMs(year: Int): YearStartAndEndEpochMs? {
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
