package au.com.shiftyjelly.pocketcasts.repositories.external

import au.com.shiftyjelly.pocketcasts.models.db.dao.ExternalDataDao
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcastMap
import javax.inject.Inject

class ExternalDataManagerImpl @Inject constructor(
    private val externalDataDao: ExternalDataDao,
) : ExternalDataManager {
    override suspend fun getRecentlyPlayedPodcasts(limit: Int): List<ExternalPodcast> {
        return externalDataDao.getRecentlyPlayedPodcasts(limit)
    }

    override suspend fun getCuratedPodcastGroups(limitPerGroup: Int): ExternalPodcastMap {
        return externalDataDao.getCuratedPodcastGroups(limitPerGroup)
    }

    override suspend fun getNewEpisodes(limit: Int): List<ExternalEpisode.Podcast> {
        return externalDataDao.getNewEpisodes(limit)
    }

    override suspend fun getInProgressEpisodes(limit: Int): List<ExternalEpisode.Podcast> {
        return externalDataDao.getInProgressEpisodes(limit)
    }
}
