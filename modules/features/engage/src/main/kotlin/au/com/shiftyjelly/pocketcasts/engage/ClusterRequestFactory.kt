package au.com.shiftyjelly.pocketcasts.engage

import android.net.Uri
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

    fun createContinuation(): PublishContinuationClusterRequest {
        val unfinishedEpisodes = List(Random.nextInt(1, 5)) { index ->
            PodcastEpisodeEntity.Builder()
                .setName("Continue listening: $index")
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
                .setListenNextType(ListenNextType.TYPE_CONTINUE)
                .setLastEngagementTimeMillis(Instant.now().minus(Random.nextLong(1, 14), ChronoUnit.DAYS).toEpochMilli())
                .setProgressPercentComplete(Random.nextInt(15, 76))
                .build()
        }
        val unfinishedCluster = ContinuationCluster.Builder()
            .setSyncAcrossDevices(false)
            .apply { unfinishedEpisodes.forEach { addEntity(it) } }
            .build()
        return PublishContinuationClusterRequest.Builder()
            .setContinuationCluster(unfinishedCluster)
            .build()
    }

    fun createFeatured(): PublishFeaturedClusterRequest {
        val featuredPodcasts = List(Random.nextInt(1, 5)) { index ->
            PodcastSeriesEntity.Builder()
                .setName("Featured: $index")
                .addPosterImage(Image.Builder().setImageUri(Uri.parse("https://dummyimage.com/400x400&text=${index + 1}")).build())
                .setInfoPageUri(Uri.parse("https://pca.st/podcast/podcast-id-$index?source=engage"))
                .setEpisodeCount(Random.nextInt(20, 200))
                .addGenres(List(Random.nextInt(0, 4)) { "Genre: $it" })
                .setDescription("Description: $index")
                .build()
        }
        val featuredCluster = FeaturedCluster.Builder()
            .apply { featuredPodcasts.forEach { addEntity(it) } }
            .build()
        return PublishFeaturedClusterRequest.Builder()
            .setFeaturedCluster(featuredCluster)
            .build()
    }
}
