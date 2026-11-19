package au.com.shiftyjelly.pocketcasts.repositories.endofyear

import java.time.Year

interface EndOfYearManager {
    suspend fun isEligibleForEndOfYear(year: Year = YEAR_TO_SYNC): Boolean
    suspend fun getStats(year: Year = YEAR_TO_SYNC): EndOfYearStats
    suspend fun getPlaybackStats(year: Year): PlaybackStats
    suspend fun getPlayedEpisodeCount(year: Year = YEAR_TO_SYNC): Int

    companion object {
        val YEAR_TO_SYNC = Year.of(2025)
    }
}
