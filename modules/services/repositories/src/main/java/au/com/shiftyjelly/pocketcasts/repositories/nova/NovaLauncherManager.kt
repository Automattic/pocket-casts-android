package au.com.shiftyjelly.pocketcasts.repositories.nova

import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherInProgressEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherNewEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherQueueEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherRecentlyPlayedPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherSubscribedPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherTrendingPodcast
import kotlinx.coroutines.flow.Flow

interface NovaLauncherManager {
    suspend fun getSubscribedPodcasts(limit: Int): List<NovaLauncherSubscribedPodcast>
    suspend fun getRecentlyPlayedPodcasts(limit: Int): List<NovaLauncherRecentlyPlayedPodcast>
    suspend fun getTrendingPodcasts(limit: Int): List<NovaLauncherTrendingPodcast>
    suspend fun getNewEpisodes(limit: Int): List<NovaLauncherNewEpisode>
    suspend fun getInProgressEpisodes(limit: Int): List<NovaLauncherInProgressEpisode>
    fun getQueueEpisodes(limit: Int): Flow<List<NovaLauncherQueueEpisode>>
}
