package au.com.shiftyjelly.pocketcasts.repositories.external

import au.com.shiftyjelly.pocketcasts.models.entity.ExternalEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcastMap

interface ExternalDataManager {
    suspend fun getRecentlyPlayedPodcasts(limit: Int): List<ExternalPodcast>
    suspend fun getCuratedPodcastGroups(limitPerGroup: Int): ExternalPodcastMap
    suspend fun getNewEpisodes(limit: Int): List<ExternalEpisode.Podcast>
    suspend fun getInProgressEpisodes(limit: Int): List<ExternalEpisode.Podcast>
}
