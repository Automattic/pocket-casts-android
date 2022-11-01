package au.com.shiftyjelly.pocketcasts.repositories.endofyear

import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import kotlinx.coroutines.flow.Flow

interface EndOfYearManager {
    fun findRandomPodcasts(): Flow<List<Podcast>>

    fun findRandomEpisode(): Flow<Episode?>

    fun getTotalListeningTimeInSecsForYear(year: Int): Flow<Long?>
}
