package au.com.shiftyjelly.pocketcasts.engage

import android.net.Uri
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.deeplink.ShowEpisodeDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ShowPodcastDeepLink
import au.com.shiftyjelly.pocketcasts.engage.BuildConfig.SERVER_SHORT_HOST
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcastList
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcastView
import com.google.android.engage.audio.datamodel.ListenNextType
import com.google.android.engage.audio.datamodel.PodcastEpisodeEntity
import com.google.android.engage.audio.datamodel.PodcastSeriesEntity
import com.google.android.engage.common.datamodel.ContinuationCluster
import com.google.android.engage.common.datamodel.FeaturedCluster
import com.google.android.engage.common.datamodel.Image
import com.google.android.engage.common.datamodel.RecommendationCluster
import com.google.android.engage.service.PublishContinuationClusterRequest
import com.google.android.engage.service.PublishFeaturedClusterRequest
import com.google.android.engage.service.PublishRecommendationClustersRequest
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

internal class ClusterRequestFactory {
    fun createRecommendations(): PublishRecommendationClustersRequest {
        val trendingPodcasts = List(Random.nextInt(1, 5)) { index ->
            PodcastSeriesEntity.Builder()
                .setName("Recommended: $index")
                .addPosterImage(Image.Builder().setImageUri(Uri.parse("https://dummyimage.com/400x400&text=${index + 1}")).build())
                .setInfoPageUri(Uri.parse("https://pca.st/podcast/podcast-id-$index?source=engage"))
                .setEpisodeCount(Random.nextInt(20, 200))
                .addGenres(List(Random.nextInt(0, 4)) { "Genre: $it" })
                .setDescription("Description: $index")
                .build()
        }
        val recentlyPlayedPodcasts = List(Random.nextInt(1, 5)) { index ->
            PodcastSeriesEntity.Builder()
                .setName("Recently played: $index")
                .addPosterImage(Image.Builder().setImageUri(Uri.parse("https://dummyimage.com/400x400&text=${index + 1}")).build())
                .setInfoPageUri(Uri.parse("https://pca.st/podcast/podcast-id-$index?source=engage"))
                .setEpisodeCount(Random.nextInt(20, 200))
                .addGenres(List(Random.nextInt(0, 4)) { "Genre: $it" })
                .setDescription("Description: $index")
                .setLastEngagementTimeMillis(Instant.now().minus(Random.nextLong(1, 14), ChronoUnit.DAYS).toEpochMilli())
                .build()
        }
        val newReleases = List(Random.nextInt(1, 5)) { index ->
            PodcastEpisodeEntity.Builder()
                .setName("New release: $index")
                .addPosterImage(Image.Builder().setImageUri(Uri.parse("https://dummyimage.com/400x400&text=${index + 1}")).build())
                .setPlayBackUri(Uri.parse("https://pca.st/episode/episode-id-$index?source=engage&autoplay=true"))
                .setPodcastSeriesTitle("Podcast title: $index")
                .setDurationMillis(Random.nextLong(25.minutes.inWholeMilliseconds, 40.minutes.inWholeMilliseconds))
                .setPublishDateEpochMillis(Instant.now().minus(Random.nextLong(1, 14), ChronoUnit.DAYS).toEpochMilli())
                .setInfoPageUri(Uri.parse("https://pca.st/episode/episode-id-$index?source=engage"))
                .setEpisodeIndex(Random.nextInt(1, 20))
                .setDownloadedOnDevice(Random.nextInt(0, 100) > 50)
                .setDescription("Description: $index")
                .setVideoPodcast(Random.nextInt(0, 100) > 50)
                .setListenNextType(ListenNextType.TYPE_NEW)
                .setLastEngagementTimeMillis(Instant.now().minus(Random.nextLong(1, 14), ChronoUnit.DAYS).toEpochMilli())
                .setProgressPercentComplete(0)
                .build()
        }
        val trendingCluster = RecommendationCluster.Builder()
            .setTitle("Trending")
            .setSubtitle("Tredning subtitle")
            .setActionUri(Uri.parse("https://lists.pocketcasts.com/trending"))
            .setActionText("Trending action")
            .apply { trendingPodcasts.forEach { addEntity(it) } }
            .build()
        val recentlyPlayedCluster = RecommendationCluster.Builder()
            .setTitle("Recently played")
            .setSubtitle("Recently played subtitle")
            .apply { recentlyPlayedPodcasts.forEach { addEntity(it) } }
            .build()
        val newReleasesCluster = RecommendationCluster.Builder()
            .setTitle("New releaes")
            .setSubtitle("New releaes subtitle")
            .apply { newReleases.forEach { addEntity(it) } }
            .build()
        return PublishRecommendationClustersRequest.Builder()
            .addRecommendationCluster(trendingCluster)
            .addRecommendationCluster(recentlyPlayedCluster)
            .addRecommendationCluster(newReleasesCluster)
            .build()
    }

    fun createContinuation(
        episodes: List<ExternalEpisode.Podcast>,
    ): PublishContinuationClusterRequest {
        val unfinishedEpisodes = episodes.map { episode ->
            PodcastEpisodeEntity.Builder()
                .setName(episode.title)
                .addPosterImage(Image.Builder().setImageUri(Uri.parse(episode.coverUrl)).build())
                .setPlayBackUri(episode.continuationUri(autoPlay = true))
                .setPodcastSeriesTitle(episode.podcastTitle)
                .setDurationMillis(episode.durationMs)
                .setPublishDateEpochMillis(episode.releaseTimestampMs)
                .setInfoPageUri(episode.continuationUri(autoPlay = false))
                .addEpisodeIndex(episode.episodeNumber)
                .setDownloadedOnDevice(episode.isDownloaded)
                .setVideoPodcast(episode.isVideo)
                .setListenNextType(ListenNextType.TYPE_CONTINUE)
                .addLastEngagementTimeMillis(episode.lastUsedTimestampMs)
                .setProgressPercentComplete(episode.percentComplete.roundToInt())
                .build()
        }
        val unfinishedCluster = ContinuationCluster.Builder()
            .apply { unfinishedEpisodes.forEach { addEntity(it) } }
            .build()
        return PublishContinuationClusterRequest.Builder()
            .setContinuationCluster(unfinishedCluster)
            .build()
    }

    fun createFeatured(
        featuredList: ExternalPodcastList?,
    ): PublishFeaturedClusterRequest {
        val featuredPodcasts = featuredList?.podcasts.orEmpty().map { podcast ->
            PodcastSeriesEntity.Builder()
                .setName(podcast.title)
                .addPosterImage(Image.Builder().setImageUri(Uri.parse(podcast.coverUrl)).build())
                .setInfoPageUri(podcast.featuredUri)
                .addDescription(podcast.description)
                .build()
        }
        val featuredCluster = FeaturedCluster.Builder()
            .apply { featuredPodcasts.forEach { addEntity(it) } }
            .build()
        return PublishFeaturedClusterRequest.Builder()
            .setFeaturedCluster(featuredCluster)
            .build()
    }

    private fun ExternalEpisode.Podcast.continuationUri(autoPlay: Boolean) = ShowEpisodeDeepLink(
        episodeUuid = id,
        podcastUuid = podcastId,
        sourceView = SourceView.ENGAGE_SDK_CONTINUATION.analyticsValue,
        autoPlay = autoPlay,
    ).toUri(SERVER_SHORT_HOST)

    private val ExternalPodcastView.featuredUri get() = ShowPodcastDeepLink(
        podcastUuid = id,
        sourceView = SourceView.ENGAGE_SDK_FEATURED.analyticsValue,
    ).toUri(SERVER_SHORT_HOST)

    private fun PodcastSeriesEntity.Builder.addDescription(description: String?): PodcastSeriesEntity.Builder {
        return if (description != null) {
            val fittingDescription = if (description.length > 200) description.take(199) + "…" else description
            setDescription(fittingDescription)
        } else {
            this
        }
    }

    private fun PodcastEpisodeEntity.Builder.addEpisodeIndex(index: Int?): PodcastEpisodeEntity.Builder {
        return if (index != null) {
            setEpisodeIndex(index)
        } else {
            this
        }
    }

    private fun PodcastEpisodeEntity.Builder.addLastEngagementTimeMillis(time: Long?): PodcastEpisodeEntity.Builder {
        return if (time != null) {
            setLastEngagementTimeMillis(time)
        } else {
            this
        }
    }
}
