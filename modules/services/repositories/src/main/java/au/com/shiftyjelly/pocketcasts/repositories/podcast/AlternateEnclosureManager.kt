package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeAlternateEnclosure
import kotlinx.coroutines.flow.Flow

interface AlternateEnclosureManager {
    suspend fun findForEpisode(episodeUuid: String): List<EpisodeAlternateEnclosure>

    fun hasHlsAlternateEnclosure(episodeUuid: String): Flow<Boolean>
}
