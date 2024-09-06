package au.com.shiftyjelly.pocketcasts.engage

import au.com.shiftyjelly.pocketcasts.models.entity.ExternalEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcastList
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcastMap
import com.google.android.engage.service.AppEngagePublishStatusCode

internal data class EngageData(
    val recommendations: Recommendations,
    val continuation: Continuation,
    val featured: Featured,
) {
    val publishStatus get() = if (recommendations.isEmpty() && continuation.isEmpty() && featured.isEmpty()) {
        AppEngagePublishStatusCode.NOT_PUBLISHED_NO_ELIGIBLE_CONTENT
    } else {
        AppEngagePublishStatusCode.PUBLISHED
    }

    companion object {
        fun create(
            recentlyPlayed: List<ExternalPodcast>,
            newReleases: List<ExternalEpisode.Podcast>,
            continuation: List<ExternalEpisode.Podcast>,
            curatedPodcasts: ExternalPodcastMap,
        ): EngageData {
            val trending = curatedPodcasts.trendingGroup(limit = 10) ?: ExternalPodcastList("", "", emptyList())
            val recommendations = curatedPodcasts.genericGroups(limit = 50)
            val featured = curatedPodcasts.featuruedGroup(limit = 10) ?: ExternalPodcastList("", "", emptyList())

            return EngageData(
                Recommendations.create(recentlyPlayed, newReleases, trending, recommendations),
                Continuation(continuation),
                Featured(featured),
            )
        }
    }

    data class Recommendations(
        val recentlyPlayed: List<ExternalPodcast>,
        val newReleases: List<ExternalEpisode.Podcast>,
        val trending: ExternalPodcastList,
        val curatedRecommendations: List<ExternalPodcastList>,
    ) {
        fun isEmpty() = recentlyPlayed.isEmpty() && newReleases.isEmpty() && trending.podcasts.isEmpty() && curatedRecommendations.isEmpty()

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

    data class Continuation(
        val episodes: List<ExternalEpisode.Podcast>,
    ) {
        fun isEmpty() = episodes.isEmpty()
    }

    data class Featured(
        val list: ExternalPodcastList,
    ) {
        fun isEmpty() = list.podcasts.isEmpty()
    }
}
