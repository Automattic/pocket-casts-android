package au.com.shiftyjelly.pocketcasts.engage

import android.content.Context
import android.net.Uri
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.deeplink.ShareListDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ShowEpisodeDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ShowPodcastDeepLink
import au.com.shiftyjelly.pocketcasts.engage.BuildConfig.SERVER_LIST_HOST
import au.com.shiftyjelly.pocketcasts.engage.BuildConfig.SERVER_SHORT_HOST
import au.com.shiftyjelly.pocketcasts.engage.EngageSdkBridge.Companion.TAG
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
import com.google.android.engage.service.AppEngagePublishClient
import com.google.android.engage.service.PublishContinuationClusterRequest
import com.google.android.engage.service.PublishFeaturedClusterRequest
import com.google.android.engage.service.PublishRecommendationClustersRequest
import com.google.android.engage.service.PublishStatusRequest
import com.google.android.gms.tasks.Task
import kotlin.math.roundToInt
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal class ClusterService(
    private val context: Context,
    private val client: AppEngagePublishClient,
) {
    fun updateRecommendations(
        data: EngageData,
    ): Task<Void> {
        val recommendations = data.recommendations

        val recentlyPlayed = recommendations.recentlyPlayed.takeIf { it.isNotEmpty() }?.let { podcasts ->
            val entities = podcasts.map { podcast ->
                PodcastSeriesEntity.Builder()
                    .setName(podcast.title)
                    .addPosterImage(
                        Image.Builder()
                            .setImageUri(Uri.parse(podcast.coverUrl))
                            .setImageWidthInPixel(podcast.coverSize)
                            .setImageHeightInPixel(podcast.coverSize)
                            .build(),
                    )
                    .setInfoPageUri(podcast.uri(SourceView.ENGAGE_SDK_RECOMMENDATIONS))
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
        val newReleases = recommendations.newReleases.takeIf { it.isNotEmpty() }?.let { episodes ->
            val entities = episodes.map { episode ->
                PodcastEpisodeEntity.Builder()
                    .setName(episode.title)
                    .addPosterImage(
                        Image.Builder()
                            .setImageUri(Uri.parse(episode.coverUrl))
                            .setImageWidthInPixel(episode.coverSize)
                            .setImageHeightInPixel(episode.coverSize)
                            .build(),
                    )
                    .setPlayBackUri(episode.uri(autoPlay = true, SourceView.ENGAGE_SDK_RECOMMENDATIONS))
                    .setPodcastSeriesTitle(episode.podcastTitle)
                    .setDurationMillis(episode.durationMs)
                    .setPublishDateEpochMillis(episode.releaseTimestampMs)
                    .setInfoPageUri(episode.uri(autoPlay = false, SourceView.ENGAGE_SDK_RECOMMENDATIONS))
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
        val trending = recommendations.trending.let { list ->
            val entities = list.podcasts.map { podcast ->
                PodcastSeriesEntity.Builder()
                    .setName(podcast.title)
                    .addPosterImage(
                        Image.Builder()
                            .setImageUri(Uri.parse(podcast.coverUrl))
                            .setImageWidthInPixel(podcast.coverSize)
                            .setImageHeightInPixel(podcast.coverSize)
                            .build(),
                    )
                    .setInfoPageUri(podcast.uri(SourceView.ENGAGE_SDK_RECOMMENDATIONS))
                    .addDescription(podcast.description)
                    .build()
            }
            RecommendationCluster.Builder()
                .setTitle(context.getString(LR.string.engage_sdk_trending))
                .setActionUri(list.uri(SourceView.ENGAGE_SDK_RECOMMENDATIONS))
                .apply { entities.forEach { addEntity(it) } }
                .build()
        }
        val curated = recommendations.curatedRecommendations.map { recommendation ->
            recommendation.takeIf { it.podcasts.isNotEmpty() }?.let { list ->
                val entities = list.podcasts.map { podcast ->
                    PodcastSeriesEntity.Builder()
                        .setName(podcast.title)
                        .addPosterImage(
                            Image.Builder()
                                .setImageUri(Uri.parse(podcast.coverUrl))
                                .setImageWidthInPixel(podcast.coverSize)
                                .setImageHeightInPixel(podcast.coverSize)
                                .build(),
                        )
                        .setInfoPageUri(podcast.uri(SourceView.ENGAGE_SDK_RECOMMENDATIONS))
                        .addDescription(podcast.description)
                        .build()
                }
                RecommendationCluster.Builder()
                    .setTitle(list.title)
                    .setActionUri(list.uri(SourceView.ENGAGE_SDK_RECOMMENDATIONS))
                    .apply { entities.forEach { addEntity(it) } }
                    .build()
            }
        }.filterNotNull()

        val clusters = listOfNotNull(
            recentlyPlayed,
            newReleases,
            trending,
        ) + curated

        return if (clusters.isNotEmpty()) {
            val request = PublishRecommendationClustersRequest.Builder()
                .let { builder ->
                    clusters.forEach { builder.addRecommendationCluster(it) }
                    builder
                }
                .build()
            updateClient(data) {
                Timber.tag(TAG).d("Publish recommendations cluster")
                client.publishRecommendationClusters(request)
            }
        } else {
            updateClient(data) {
                Timber.tag(TAG).d("Delete recommendations cluster")
                client.deleteRecommendationsClusters()
            }
        }
    }

    fun updateContinuation(
        data: EngageData,
    ): Task<Void> {
        val episodes = data.continuation.episodes

        val unfinishedEpisodes = episodes.map { episode ->
            PodcastEpisodeEntity.Builder()
                .setName(episode.title)
                .addPosterImage(
                    Image.Builder()
                        .setImageUri(Uri.parse(episode.coverUrl))
                        .setImageWidthInPixel(episode.coverSize)
                        .setImageHeightInPixel(episode.coverSize)
                        .build(),
                )
                .setPlayBackUri(episode.uri(autoPlay = true, SourceView.ENGAGE_SDK_CONTINUATION))
                .setPodcastSeriesTitle(episode.podcastTitle)
                .setDurationMillis(episode.durationMs)
                .setPublishDateEpochMillis(episode.releaseTimestampMs)
                .setInfoPageUri(episode.uri(autoPlay = false, SourceView.ENGAGE_SDK_CONTINUATION))
                .addEpisodeIndex(episode.episodeNumber)
                .setDownloadedOnDevice(episode.isDownloaded)
                .setVideoPodcast(episode.isVideo)
                .setListenNextType(ListenNextType.TYPE_CONTINUE)
                .addLastEngagementTimeMillis(episode.lastUsedTimestampMs)
                .setProgressPercentComplete(episode.percentComplete.roundToInt())
                .build()
        }

        return if (unfinishedEpisodes.isNotEmpty()) {
            val cluster = ContinuationCluster.Builder()
                .apply { unfinishedEpisodes.forEach { addEntity(it) } }
                .build()
            val request = PublishContinuationClusterRequest.Builder()
                .setContinuationCluster(cluster)
                .build()
            updateClient(data) {
                Timber.tag(TAG).d("Publish continuation cluster")
                client.publishContinuationCluster(request)
            }
        } else {
            updateClient(data) {
                Timber.tag(TAG).d("Delete continuation cluster")
                client.deleteContinuationCluster()
            }
        }
    }

    fun updateFeatured(
        data: EngageData,
    ): Task<Void> {
        val featuredList = data.featured.list

        val featuredPodcasts = featuredList.podcasts.map { podcast ->
            PodcastSeriesEntity.Builder()
                .setName(podcast.title)
                .addPosterImage(
                    Image.Builder()
                        .setImageUri(Uri.parse(podcast.coverUrl))
                        .setImageWidthInPixel(podcast.coverSize)
                        .setImageHeightInPixel(podcast.coverSize)
                        .build(),
                )
                .setInfoPageUri(podcast.uri(SourceView.ENGAGE_SDK_FEATURED))
                .addDescription(podcast.description)
                .build()
        }

        return if (featuredPodcasts.isNotEmpty()) {
            val featuredCluster = FeaturedCluster.Builder()
                .apply { featuredPodcasts.forEach { addEntity(it) } }
                .build()
            val request = PublishFeaturedClusterRequest.Builder()
                .setFeaturedCluster(featuredCluster)
                .build()
            updateClient(data) {
                Timber.tag(TAG).d("Publish featured cluster")
                client.publishFeaturedCluster(request)
            }
        } else {
            updateClient(data) {
                Timber.tag(TAG).d("Delete featured cluster")
                client.deleteFeaturedCluster()
            }
        }
    }

    private fun updateClient(
        data: EngageData,
        task: () -> Task<Void>,
    ): Task<Void> {
        return task().continueWithTask {
            Timber.tag(TAG).d("Update publish status: ${data.publishStatus}")
            val stausRequest = PublishStatusRequest.Builder().setStatusCode(data.publishStatus).build()
            client.updatePublishStatus(stausRequest)
        }
    }

    private fun ExternalPodcast.uri(source: SourceView) = ShowPodcastDeepLink(
        podcastUuid = id,
        sourceView = source.analyticsValue,
    ).toUri(SERVER_SHORT_HOST)

    private fun ExternalEpisode.Podcast.uri(autoPlay: Boolean, source: SourceView) = ShowEpisodeDeepLink(
        episodeUuid = id,
        podcastUuid = podcastId,
        sourceView = source.analyticsValue,
        autoPlay = autoPlay,
    ).toUri(SERVER_SHORT_HOST)

    private fun ExternalPodcastList.uri(source: SourceView) = ShareListDeepLink(
        path = id,
        sourceView = source.analyticsValue,
    ).toUri(SERVER_LIST_HOST)

    private fun ExternalPodcastView.uri(source: SourceView) = ShowPodcastDeepLink(
        podcastUuid = id,
        sourceView = source.analyticsValue,
    ).toUri(SERVER_SHORT_HOST)

    private fun PodcastSeriesEntity.Builder.addDescription(description: String?): PodcastSeriesEntity.Builder {
        return if (description != null) {
            val fittingDescription = if (description.length > 200) description.take(199) + "…" else description
            setDescription(fittingDescription)
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
