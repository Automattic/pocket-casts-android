package au.com.shiftyjelly.pocketcasts.repositories.nova

import au.com.shiftyjelly.pocketcasts.models.entity.ExternalEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcastMap
import kotlinx.coroutines.flow.Flow

interface ExternalDataManager {
    suspend fun getSubscribedPodcasts(limit: Int): List<ExternalPodcast>
    suspend fun getRecentlyPlayedPodcasts(limit: Int): List<ExternalPodcast>
    suspend fun getCuratedPodcastGroups(limitPerGroup: Int): ExternalPodcastMap
    suspend fun getNewEpisodes(limit: Int): List<ExternalEpisode.Podcast>
    suspend fun getInProgressEpisodes(limit: Int): List<ExternalEpisode.Podcast>
    fun observeUpNextQueue(limit: Int): Flow<List<ExternalEpisode>>
}
