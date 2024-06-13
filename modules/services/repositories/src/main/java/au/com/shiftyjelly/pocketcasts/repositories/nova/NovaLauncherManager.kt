package au.com.shiftyjelly.pocketcasts.repositories.nova

import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherInProgressEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherNewEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherRecentlyPlayedPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherSubscribedPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherTrendingPodcast

interface NovaLauncherManager {
    suspend fun getSubscribedPodcasts(limit: Int): List<NovaLauncherSubscribedPodcast>
    suspend fun getRecentlyPlayedPodcasts(): List<NovaLauncherRecentlyPlayedPodcast>
    suspend fun getTrendingPodcasts(): List<NovaLauncherTrendingPodcast>
    suspend fun getNewEpisodes(): List<NovaLauncherNewEpisode>
    suspend fun getInProgressEpisodes(): List<NovaLauncherInProgressEpisode>
}
