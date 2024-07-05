package au.com.shiftyjelly.pocketcasts.nova

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.deeplink.ShowPodcastDeepLink
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherInProgressEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherNewEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherQueueEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherRecentlyPlayedPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherSubscribedPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherTrendingPodcast
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.preferences.Settings
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
    fun subscribedPodcasts(data: List<NovaLauncherSubscribedPodcast>) = PodcastSeriesCatalog("SubscribedPodcasts")
        .setLabel(context.getString(LR.string.nova_launcher_subscribed_podcasts))
        .setPreferredAspectRatio(1, 1)
        .addAllItems(
            data.mapIndexed { index, podcast ->
                PodcastSeries(podcast.id)
                    .setRank(index.toLong()) // Our queries sort podcasts in a desired order.
                    .setOpensDirectlyTo(podcast.intent)
                    .setName(podcast.title)
                    .setIcon(Image.WebUrl(podcast.coverUrl, 1 to 1))
                    .setLastUsedTimestamp(podcast.lastUsedTimestamp)
                    .setOriginalReleaseTimestamp(podcast.initialReleaseTimestamp)
                    .setLatestReleaseTimestamp(podcast.latestReleaseTimestamp)
                    .addAllCategories(ApplePodcastCategory.fromCategories(podcast.categories))
            },
        )

    fun recentlyPlayedPodcasts(data: List<NovaLauncherRecentlyPlayedPodcast>) = PodcastSeriesCatalog("RecentlyPlayed")
        .setLabel(context.getString(LR.string.nova_launcher_recently_played))
        .setPreferredAspectRatio(1, 1)
        .addAllItems(
            data.mapIndexed { index, podcast ->
                PodcastSeries(podcast.id)
                    .setRank(index.toLong()) // Our queries sort podcasts in a desired order.
                    .setOpensDirectlyTo(podcast.intent)
                    .setName(podcast.title)
                    .setIcon(Image.WebUrl(podcast.coverUrl, 1 to 1))
                    .setLastUsedTimestamp(podcast.lastUsedTimestamp)
                    .setOriginalReleaseTimestamp(podcast.initialReleaseTimestamp)
                    .setLatestReleaseTimestamp(podcast.latestReleaseTimestamp)
                    .addAllCategories(ApplePodcastCategory.fromCategories(podcast.categories))
            },
        )

    fun trendingPodcasts(data: List<NovaLauncherTrendingPodcast>) = PodcastSeriesCatalog("TrendingPodcasts")
        .setLabel(context.getString(LR.string.nova_launcher_trending))
        .setPreferredAspectRatio(1, 1)
        .addAllItems(
            data.mapIndexed { index, podcast ->
                PodcastSeries(podcast.id)
                    .setRank(index.toLong()) // Our queries sort podcasts in a desired order.
                    .setOpensDirectlyTo(podcast.intent)
                    .setName(podcast.title)
                    .setIcon(Image.WebUrl(podcast.coverUrl, 1 to 1))
            },
        )

    fun newEpisodes(data: List<NovaLauncherNewEpisode>) = PodcastEpisodeCatalog("NewReleases")
        .setLabel(context.getString(LR.string.nova_launcher_new_releases))
        .setPreferredAspectRatio(1, 1)
        .addAllItems(
            data.mapIndexed { index, episode ->
                PodcastEpisode(episode.id)
                    .setRank(index.toLong()) // Our queries sort episodes in a desired order.
                    .setOpensDirectlyTo(episode.intent)
                    .setLastUsedTimestamp(episode.lastUsedTimestamp)
                    .setName(episode.title)
                    .setIcon(Image.WebUrl(episode.coverUrl, 1 to 1))
                    .setSeasonNumber(episode.seasonNumber)
                    .setEpisodeNumber(episode.episodeNumber)
                    .setReleaseTimestamp(episode.releaseTimestamp)
                    .setLengthSeconds(episode.duration)
                    .setCurrentPositionSeconds(episode.currentPosition)
            },
        )

    fun inProgressEpisodes(data: List<NovaLauncherInProgressEpisode>) = PodcastEpisodeCatalog("ContinueListening")
        .setLabel(context.getString(LR.string.nova_launcher_continue_listening))
        .setPreferredAspectRatio(1, 1)
        .addAllItems(
            data.mapIndexed { index, episode ->
                PodcastEpisode(episode.id)
                    .setRank(index.toLong()) // Our queries sort episodes in a desired order.
                    .setOpensDirectlyTo(episode.intent)
                    .setLastUsedTimestamp(episode.lastUsedTimestamp)
                    .setName(episode.title)
                    .setIcon(Image.WebUrl(episode.coverUrl, 1 to 1))
                    .setSeasonNumber(episode.seasonNumber)
                    .setEpisodeNumber(episode.episodeNumber)
                    .setReleaseTimestamp(episode.releaseTimestamp)
                    .setLengthSeconds(episode.duration)
                    .setCurrentPositionSeconds(episode.currentPosition)
            },
        )

    fun queuedEpisodes(data: List<NovaLauncherQueueEpisode>) = PodcastEpisodeCatalog("UpNextQueue")
        .setLabel(context.getString(LR.string.nova_launcher_up_next))
        .setPreferredAspectRatio(1, 1)
        .addAllItems(
            data.mapIndexed { index, episode ->
                PodcastEpisode(episode.id)
                    .setRank(index.toLong()) // Our queries sort episodes in a desired order.
                    .setOpensDirectlyTo(episode.intent)
                    .setName(episode.title)
                    .setIcon(Image.WebUrl(episode.coverUrl, 1 to 1))
                    .setReleaseTimestamp(episode.releaseTimestamp)
                    .setLengthSeconds(episode.duration)
                    .setCurrentPositionSeconds(episode.currentPosition)
                    .setLastUsedTimestamp(episode.lastUsedTimestamp)
                    .setSeasonNumber(episode.seasonNumber)
                    .setEpisodeNumber(episode.episodeNumber)
            },
        )

    private val NovaLauncherSubscribedPodcast.coverUrl get() = podcastCover(id)

    private val NovaLauncherSubscribedPodcast.intent get() = openPodcastIntent(id, SourceView.NOVA_LAUNCHER_SUBSCRIBED_PODCASTS)

    private val NovaLauncherRecentlyPlayedPodcast.coverUrl get() = podcastCover(id)

    private val NovaLauncherRecentlyPlayedPodcast.intent get() = openPodcastIntent(id, SourceView.NOVA_LAUNCHER_RECENTLY_PLAYED)

    private val NovaLauncherTrendingPodcast.coverUrl get() = podcastCover(id)

    private val NovaLauncherTrendingPodcast.intent get() = openPodcastIntent(id, SourceView.NOVA_LAUNCHER_TRENDING_PODCASTS)

    private val NovaLauncherNewEpisode.coverUrl get() = podcastCover(podcastId)

    private val NovaLauncherNewEpisode.intent get() = openPodcastEpisodeIntent(episodeId = id, podcastId = podcastId, EpisodeViewSource.NOVA_LAUNCHER_NEW_RELEASES)

    private val NovaLauncherInProgressEpisode.intent get() = openPodcastEpisodeIntent(episodeId = id, podcastId = podcastId, EpisodeViewSource.NOVA_LAUNCHER_IN_PROGRESS)

    private val NovaLauncherInProgressEpisode.coverUrl get() = podcastCover(podcastId)

    private val NovaLauncherQueueEpisode.intent get() = if (isPodcastEpisode) {
        openPodcastEpisodeIntent(episodeId = id, podcastId = requireNotNull(podcastId), EpisodeViewSource.NOVA_LAUNCHER_QUEUE)
    } else {
        openUserEpisodeIntent(id, EpisodeViewSource.NOVA_LAUNCHER_QUEUE)
    }

    private val NovaLauncherQueueEpisode.coverUrl get() = if (isPodcastEpisode) {
        podcastCover(requireNotNull(podcastId))
    } else {
        artworkUrl?.takeIf { tintColorIndex == 0 } ?: "${Settings.SERVER_STATIC_URL}/discover/images/artwork/dark/960/$tintColorIndex.png"
    }

    private fun podcastCover(podcastId: String) = "${Settings.SERVER_STATIC_URL}/discover/images/webp/960/$podcastId.webp"

    private fun openPodcastIntent(podcastId: String, sourceView: SourceView) = ShowPodcastDeepLink(
        podcastUuid = podcastId,
        sourceView = sourceView.analyticsValue,
    ).toIntent(context)

    private fun openPodcastEpisodeIntent(episodeId: String, podcastId: String, source: EpisodeViewSource) = context.launcherIntent
        .setAction(Settings.INTENT_OPEN_APP_EPISODE_UUID)
        .putExtra(Settings.EPISODE_UUID, episodeId)
        .putExtra(Settings.PODCAST_UUID, podcastId)
        .putExtra(Settings.SOURCE_VIEW, source.value)

    private fun openUserEpisodeIntent(episodeId: String, source: EpisodeViewSource) = context.launcherIntent
        .setAction(Settings.INTENT_OPEN_APP_EPISODE_UUID)
        .putExtra(Settings.EPISODE_UUID, episodeId)
        .putExtra(Settings.SOURCE_VIEW, source.value)

    private val Context.launcherIntent get() = requireNotNull(packageManager.getLaunchIntentForPackage(packageName)) {
        "Missing launcher intent for $packageName"
    }

    private fun ApplePodcastCategory.Companion.fromCategories(categories: String) = categories.split('\n').mapNotNull(ApplePodcastCategory::valueOfSafe)
}
