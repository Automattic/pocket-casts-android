package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeAlternateEnclosure

interface AlternateEnclosureManager {
    suspend fun findForEpisode(episodeUuid: String): List<EpisodeAlternateEnclosure>
}
