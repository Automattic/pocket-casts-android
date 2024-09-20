package au.com.shiftyjelly.pocketcasts.nova

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.deeplink.ShowEpisodeDeepLink
import au.com.shiftyjelly.pocketcasts.deeplink.ShowPodcastDeepLink
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcastView
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import io.branch.engage.conduit.source.ApplePodcastCategory
import io.branch.engage.conduit.source.Image
import io.branch.engage.conduit.source.PodcastEpisode
import io.branch.engage.conduit.source.PodcastEpisodeCatalog
import io.branch.engage.conduit.source.PodcastSeries
import io.branch.engage.conduit.source.PodcastSeriesCatalog
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal class CatalogFactory(
    private val context: Context,
) {
    fun subscribedPodcasts(data: List<ExternalPodcast>) = PodcastSeriesCatalog("SubscribedPodcasts")
        .setLabel(context.getString(LR.string.nova_launcher_subscribed_podcasts))
        .setPreferredAspectRatio(1, 1)
        .addAllItems(
            data.mapIndexed { index, podcast ->
                PodcastSeries(podcast.id)
                    .setRank(index.toLong()) // Our queries sort podcasts in a desired order.
                    .setOpensDirectlyTo(podcast.subscribedIntent)
                    .setName(podcast.title)
                    .setIcon(Image.WebUrl(podcast.coverUrl, 1 to 1))
                    .setLastUsedTimestamp(podcast.lastUsedTimestampMs)
                    .setOriginalReleaseTimestamp(podcast.initialReleaseTimestampMs)
                    .setLatestReleaseTimestamp(podcast.latestReleaseTimestampMs)
                    .addAllCategories(ApplePodcastCategory.fromCategories(podcast.categories))
            },
        )

    fun recentlyPlayedPodcasts(data: List<ExternalPodcast>) = PodcastSeriesCatalog("RecentlyPlayed")
        .setLabel(context.getString(LR.string.nova_launcher_recently_played))
        .setPreferredAspectRatio(1, 1)
        .addAllItems(
            data.mapIndexed { index, podcast ->
                PodcastSeries(podcast.id)
                    .setRank(index.toLong()) // Our queries sort podcasts in a desired order.
                    .setOpensDirectlyTo(podcast.recentlyPlayedIntent)
                    .setName(podcast.title)
                    .setIcon(Image.WebUrl(podcast.coverUrl, 1 to 1))
                    .setLastUsedTimestamp(podcast.lastUsedTimestampMs)
                    .setOriginalReleaseTimestamp(podcast.initialReleaseTimestampMs)
                    .setLatestReleaseTimestamp(podcast.latestReleaseTimestampMs)
                    .addAllCategories(ApplePodcastCategory.fromCategories(podcast.categories))
            },
        )

    fun trendingPodcasts(data: List<ExternalPodcastView>) = PodcastSeriesCatalog("TrendingPodcasts")
        .setLabel(context.getString(LR.string.nova_launcher_trending))
        .setPreferredAspectRatio(1, 1)
        .addAllItems(
            data.mapIndexed { index, podcast ->
                PodcastSeries(podcast.id)
                    .setRank(index.toLong()) // Our queries sort podcasts in a desired order.
                    .setOpensDirectlyTo(podcast.trendingIntent)
                    .setName(podcast.title)
                    .setIcon(Image.WebUrl(podcast.coverUrl, 1 to 1))
            },
        )

    fun newEpisodes(data: List<ExternalEpisode.Podcast>) = PodcastEpisodeCatalog("NewReleases")
        .setLabel(context.getString(LR.string.nova_launcher_new_releases))
        .setPreferredAspectRatio(1, 1)
        .addAllItems(
            data.mapIndexed { index, episode ->
                PodcastEpisode(episode.id)
                    .setRank(index.toLong()) // Our queries sort episodes in a desired order.
                    .setOpensDirectlyTo(episode.newReleaseIntent)
                    .setLastUsedTimestamp(episode.lastUsedTimestampMs)
                    .setName(episode.title)
                    .setIcon(Image.WebUrl(episode.coverUrl, 1 to 1))
                    .setSeasonNumber(episode.seasonNumber)
                    .setEpisodeNumber(episode.episodeNumber)
                    .setReleaseTimestamp(episode.releaseTimestampMs)
                    .setLengthSeconds(episode.durationMs)
                    .setCurrentPositionSeconds(episode.playbackPositionMs)
            },
        )

    fun inProgressEpisodes(data: List<ExternalEpisode.Podcast>) = PodcastEpisodeCatalog("ContinueListening")
        .setLabel(context.getString(LR.string.nova_launcher_continue_listening))
        .setPreferredAspectRatio(1, 1)
        .addAllItems(
            data.mapIndexed { index, episode ->
                PodcastEpisode(episode.id)
                    .setRank(index.toLong()) // Our queries sort episodes in a desired order.
                    .setOpensDirectlyTo(episode.inProgressIntent)
                    .setLastUsedTimestamp(episode.lastUsedTimestampMs)
                    .setName(episode.title)
                    .setIcon(Image.WebUrl(episode.coverUrl, 1 to 1))
                    .setSeasonNumber(episode.seasonNumber)
                    .setEpisodeNumber(episode.episodeNumber)
                    .setReleaseTimestamp(episode.releaseTimestampMs)
                    .setLengthSeconds(episode.durationMs)
                    .setCurrentPositionSeconds(episode.playbackPositionMs)
            },
        )

    fun queuedEpisodes(data: List<ExternalEpisode>) = PodcastEpisodeCatalog("UpNextQueue")
        .setLabel(context.getString(LR.string.nova_launcher_up_next))
        .setPreferredAspectRatio(1, 1)
        .addAllItems(data.mapIndexed { index, episode -> episode.toNovaEpisode(index) })

    private fun ExternalEpisode.toNovaEpisode(index: Int): PodcastEpisode {
        val base = PodcastEpisode(id)
            .setRank(index.toLong()) // Our queries sort episodes in a desired order.
            .setOpensDirectlyTo(queuedIntent)
            .setName(title)
            .setIcon(Image.WebUrl(coverUrl, 1 to 1))
            .setReleaseTimestamp(releaseTimestampMs)
            .setLengthSeconds(durationMs)
        return when (this) {
            is ExternalEpisode.Podcast ->
                base
                    .setCurrentPositionSeconds(playbackPositionMs)
                    .setLastUsedTimestamp(lastUsedTimestampMs)
                    .setSeasonNumber(seasonNumber)
                    .setEpisodeNumber(episodeNumber)
            is ExternalEpisode.User -> base
        }
    }

    private val ExternalPodcast.subscribedIntent get() = openPodcastIntent(id, SourceView.NOVA_LAUNCHER_SUBSCRIBED_PODCASTS)

    private val ExternalPodcast.recentlyPlayedIntent get() = openPodcastIntent(id, SourceView.NOVA_LAUNCHER_RECENTLY_PLAYED)

    private val ExternalPodcastView.trendingIntent get() = openPodcastIntent(id, SourceView.NOVA_LAUNCHER_TRENDING_PODCASTS)

    private val ExternalEpisode.Podcast.newReleaseIntent get() = openPodcastEpisodeIntent(episodeId = id, podcastId = podcastId, EpisodeViewSource.NOVA_LAUNCHER_NEW_RELEASES)

    private val ExternalEpisode.Podcast.inProgressIntent get() = openPodcastEpisodeIntent(episodeId = id, podcastId = podcastId, EpisodeViewSource.NOVA_LAUNCHER_IN_PROGRESS)

    private val ExternalEpisode.queuedIntent
        get() = when (this) {
            is ExternalEpisode.Podcast -> openPodcastEpisodeIntent(episodeId = id, podcastId = podcastId, EpisodeViewSource.NOVA_LAUNCHER_QUEUE)
            is ExternalEpisode.User -> openUserEpisodeIntent(id, EpisodeViewSource.NOVA_LAUNCHER_QUEUE)
        }

    private fun openPodcastIntent(podcastId: String, sourceView: SourceView) = ShowPodcastDeepLink(
        podcastUuid = podcastId,
        sourceView = sourceView.analyticsValue,
    ).toIntent(context)

    private fun openPodcastEpisodeIntent(episodeId: String, podcastId: String, source: EpisodeViewSource) = ShowEpisodeDeepLink(
        episodeUuid = episodeId,
        podcastUuid = podcastId,
        sourceView = source.value,
        autoPlay = false,
    ).toIntent(context)

    private fun openUserEpisodeIntent(episodeId: String, source: EpisodeViewSource) = ShowEpisodeDeepLink(
        episodeUuid = episodeId,
        podcastUuid = null,
        sourceView = source.value,
        autoPlay = false,
    ).toIntent(context)

    private fun ApplePodcastCategory.Companion.fromCategories(categories: List<String>) = categories.mapNotNull(ApplePodcastCategory::valueOfSafe)
}
