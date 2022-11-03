package au.com.shiftyjelly.pocketcasts.repositories.endofyear

import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedCategory
import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedNumbers
import au.com.shiftyjelly.pocketcasts.models.db.helper.TopPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import kotlinx.coroutines.flow.Flow

interface EndOfYearManager {
    fun findRandomPodcasts(): Flow<List<Podcast>>
    fun getTotalListeningTimeInSecsForYear(year: Int): Flow<Long?>
    fun findListenedCategoriesForYear(year: Int): Flow<List<ListenedCategory>>
    fun findListenedNumbersForYear(year: Int): Flow<ListenedNumbers>
    fun findTopPodcastsForYear(year: Int, limit: Int): Flow<List<TopPodcast>>
}
