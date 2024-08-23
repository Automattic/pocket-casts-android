package au.com.shiftyjelly.pocketcasts.repositories.nova

import au.com.shiftyjelly.pocketcasts.models.db.dao.ExternalDataDao
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcastMap
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ExternalDataManagerImpl @Inject constructor(
    private val externalDataDao: ExternalDataDao,
    private val settings: Settings,
) : ExternalDataManager {
    override suspend fun getSubscribedPodcasts(limit: Int): List<ExternalPodcast> {
        return externalDataDao.getSubscribedPodcasts(settings.podcastsSortType.value, limit)
    }

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

    override fun observeUpNextQueue(limit: Int): Flow<List<ExternalEpisode>> {
        return externalDataDao.observeUpNextQueue(limit)
    }
}
