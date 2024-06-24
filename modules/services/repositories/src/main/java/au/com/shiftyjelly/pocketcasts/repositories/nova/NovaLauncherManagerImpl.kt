package au.com.shiftyjelly.pocketcasts.repositories.nova

import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.UpNextDao
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import javax.inject.Inject

class NovaLauncherManagerImpl @Inject constructor(
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val upNextDao: UpNextDao,
    private val settings: Settings,
) : NovaLauncherManager {
    override suspend fun getSubscribedPodcasts(limit: Int) = podcastDao.getNovaLauncherSubscribedPodcasts(settings.podcastsSortType.value, limit)
    override suspend fun getRecentlyPlayedPodcasts(limit: Int) = podcastDao.getNovaLauncherRecentlyPlayedPodcasts(limit)
    override suspend fun getTrendingPodcasts(limit: Int) = podcastDao.getNovaLauncherTrendingPodcasts(limit)
    override suspend fun getNewEpisodes(limit: Int) = episodeDao.getNovaLauncherNewEpisodes(limit)
    override suspend fun getInProgressEpisodes(limit: Int) = episodeDao.getNovaLauncherInProgressEpisodes(limit)
    override fun getQueueEpisodes(limit: Int) = upNextDao.getNovaLauncherQueueEpisodes(limit)
}
