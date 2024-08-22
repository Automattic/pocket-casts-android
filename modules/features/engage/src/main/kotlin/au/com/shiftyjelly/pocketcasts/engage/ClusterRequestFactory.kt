package au.com.shiftyjelly.pocketcasts.engage

import android.content.Context
import android.net.Uri
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.deeplink.ShareListDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ShowEpisodeDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ShowPodcastDeepLink
import au.com.shiftyjelly.pocketcasts.engage.BuildConfig.SERVER_LIST_HOST
import au.com.shiftyjelly.pocketcasts.engage.BuildConfig.SERVER_SHORT_HOST
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcast
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
import kotlin.math.roundToInt
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal class ClusterRequestFactory(
    private val context: Context,
) {
    fun createRecommendations(
        recommendationsData: RecommendationsData,
    ): PublishRecommendationClustersRequest {
        val recentlyPlayed = recommendationsData.recentlyPlayed.takeIf { it.isNotEmpty() }?.let { podcasts ->
            val entities = podcasts.map { podcast ->
                PodcastSeriesEntity.Builder()
                    .setName(podcast.title)
                    .addPosterImage(Image.Builder().setImageUri(Uri.parse(podcast.coverUrl)).build())
                    .setInfoPageUri(podcast.recommendationsUri.also { Timber.tag("Engage").i(it.toString()) })
                    .setEpisodeCount(podcast.episodeCount)
                    .addGenres(podcast.categories)
                    .addDescription(podcast.description)
                    .addLastEngagementTimeMillis(podcast.lastUsedTimestampMs)
                    .build()
            }
            RecommendationCluster.Builder()
                .setTitle(context.getString(LR.string.engage_sdk_recently_played))
                .apply { entities.forEach { addEntity(it) } }
                .build()
        }
        val newReleases = recommendationsData.newReleases.takeIf { it.isNotEmpty() }?.let { episodes ->
            val entities = episodes.map { episode ->
                PodcastEpisodeEntity.Builder()
                    .setName(episode.title)
                    .addPosterImage(Image.Builder().setImageUri(Uri.parse(episode.coverUrl)).build())
                    .setPlayBackUri(episode.recommendationsUri(autoPlay = true))
                    .setPodcastSeriesTitle(episode.podcastTitle)
                    .setDurationMillis(episode.durationMs)
                    .setPublishDateEpochMillis(episode.releaseTimestampMs)
                    .setInfoPageUri(episode.recommendationsUri(autoPlay = false))
                    .addEpisodeIndex(episode.episodeNumber)
                    .setDownloadedOnDevice(episode.isDownloaded)
                    .setVideoPodcast(episode.isVideo)
                    .setListenNextType(ListenNextType.TYPE_NEW)
                    .addLastEngagementTimeMillis(episode.lastUsedTimestampMs)
                    .setProgressPercentComplete(episode.percentComplete.roundToInt())
                    .build()
            }
            RecommendationCluster.Builder()
                .setTitle(context.getString(LR.string.engage_sdk_new_releases))
                .apply { entities.forEach { addEntity(it) } }
                .build()
        }
        val trending = recommendationsData.trending.takeIf { it.podcasts.isNotEmpty() }?.let { list ->
            val entities = list.podcasts.map { podcast ->
                PodcastSeriesEntity.Builder()
                    .setName(podcast.title)
                    .addPosterImage(Image.Builder().setImageUri(Uri.parse(podcast.coverUrl)).build())
                    .setInfoPageUri(podcast.recommendationsUri)
                    .addDescription(podcast.description)
                    .build()
            }
            RecommendationCluster.Builder()
                .setTitle(context.getString(LR.string.engage_sdk_trending))
                .setActionUri(list.recommendationsDeepLinkUri)
                .apply { entities.forEach { addEntity(it) } }
                .build()
        }
        val curated = recommendationsData.curatedRecommendations.map { recommendation ->
            recommendation.takeIf { it.podcasts.isNotEmpty() }?.let { list ->
                val entities = list.podcasts.map { podcast ->
                    PodcastSeriesEntity.Builder()
                        .setName(podcast.title)
                        .addPosterImage(Image.Builder().setImageUri(Uri.parse(podcast.coverUrl)).build())
                        .setInfoPageUri(podcast.recommendationsUri)
                        .addDescription(podcast.description)
                        .build()
                }
                RecommendationCluster.Builder()
                    .setTitle(list.title)
                    .setActionUri(list.recommendationsDeepLinkUri)
                    .apply { entities.forEach { addEntity(it) } }
                    .build()
            }
        }.filterNotNull()

        val clusters = listOfNotNull(
            recentlyPlayed,
            newReleases,
            trending,
        ) + curated

        return PublishRecommendationClustersRequest.Builder()
            .let { builder ->
                clusters.forEach { builder.addRecommendationCluster(it) }
                builder
            }
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

    private val ExternalPodcast.recommendationsUri get() = ShowPodcastDeepLink(
        podcastUuid = id,
        sourceView = SourceView.ENGAGE_SDK_RECOMMENDATIONS.analyticsValue,
    ).toUri(SERVER_SHORT_HOST)

    private fun ExternalEpisode.Podcast.recommendationsUri(autoPlay: Boolean) = ShowEpisodeDeepLink(
        episodeUuid = id,
        podcastUuid = podcastId,
        sourceView = SourceView.ENGAGE_SDK_RECOMMENDATIONS.analyticsValue,
        autoPlay = autoPlay,
    ).toUri(SERVER_SHORT_HOST)

    private val ExternalPodcastView.recommendationsUri get() = ShowPodcastDeepLink(
        podcastUuid = id,
        sourceView = SourceView.ENGAGE_SDK_RECOMMENDATIONS.analyticsValue,
    ).toUri(SERVER_SHORT_HOST)

    private val ExternalPodcastList.recommendationsDeepLinkUri get() = ShareListDeepLink(
        path = id,
        sourceView = SourceView.ENGAGE_SDK_RECOMMENDATIONS.analyticsValue,
    ).toUri(SERVER_LIST_HOST)

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

    private fun PodcastSeriesEntity.Builder.addLastEngagementTimeMillis(time: Long?): PodcastSeriesEntity.Builder {
        return if (time != null) {
            setLastEngagementTimeMillis(time)
        } else {
            this
        }
    }
}
