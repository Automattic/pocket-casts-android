package au.com.shiftyjelly.pocketcasts.engage

import au.com.shiftyjelly.pocketcasts.models.entity.ExternalEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcastList

internal data class Recommendations(
    val recentlyPlayed: List<ExternalPodcast>,
    val newReleases: List<ExternalEpisode.Podcast>,
    val trending: ExternalPodcastList,
    val curatedRecommendations: List<ExternalPodcastList>,
) {
    companion object {
        fun create(
            recentlyPlayed: List<ExternalPodcast>,
            newReleases: List<ExternalEpisode.Podcast>,
            trending: ExternalPodcastList,
            recommendations: Map<String, ExternalPodcastList>,
        ): Recommendations {
            val sortedRecommendations = recommendations.values
                .filter { it.podcasts.isNotEmpty() }
                .sortedByDescending { it.podcasts.size }
                .take(5 - recentlyPlayed.size.coerceAtMost(1) - newReleases.size.coerceAtMost(1) - trending.podcasts.size.coerceAtMost(1))
            return Recommendations(recentlyPlayed, newReleases, trending, sortedRecommendations)
        }
    }
}
