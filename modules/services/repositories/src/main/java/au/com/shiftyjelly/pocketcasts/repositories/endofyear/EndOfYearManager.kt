package au.com.shiftyjelly.pocketcasts.repositories.endofyear

import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedCategory
import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedNumbers
import au.com.shiftyjelly.pocketcasts.models.db.helper.LongestEpisode
import au.com.shiftyjelly.pocketcasts.models.db.helper.TopPodcast
import kotlinx.coroutines.flow.Flow

interface EndOfYearManager {
    fun hasEpisodesPlayedUpto(year: Int, playedUpToInSecs: Long): Flow<Boolean>
    fun hasEpisodeInteractedBefore(year: Int): Flow<Boolean>
    fun hasListeningHistoryEpisodesInLimitForYear(year: Int, limit: Int): Flow<Boolean>
    fun getTotalListeningTimeInSecsForYear(year: Int): Flow<Long?>
    fun findListenedCategoriesForYear(year: Int): Flow<List<ListenedCategory>>
    fun findListenedNumbersForYear(year: Int): Flow<ListenedNumbers>
    fun findTopPodcastsForYear(year: Int, limit: Int): Flow<List<TopPodcast>>
    fun findLongestPlayedEpisodeForYear(year: Int): Flow<LongestEpisode?>
}
