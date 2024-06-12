package au.com.shiftyjelly.pocketcasts.repositories.nova

import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import javax.inject.Inject

class NovaLauncherManagerImpl @Inject constructor(
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val settings: Settings,
) : NovaLauncherManager {
    override suspend fun getSubscribedPodcasts(limit: Int) = podcastDao.getNovaLauncherSubscribedPodcasts(settings.podcastsSortType.value, limit = limit)
    override suspend fun getRecentlyPlayedPodcasts() = podcastDao.getNovaLauncherRecentlyPlayedPodcasts()
    override suspend fun getTrendingPodcasts() = podcastDao.getNovaLauncherTrendingPodcasts()
    override suspend fun getNewEpisodes() = episodeDao.getNovaLauncherNewEpisodes()
    override suspend fun getInProgressEpisodes() = episodeDao.getNovaLauncherInProgressEpisodes()
}
