package au.com.shiftyjelly.pocketcasts.nova

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherSubscribedPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherTrendingPodcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import io.branch.engage.conduit.source.ApplePodcastCategory
import io.branch.engage.conduit.source.Image
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

    private val NovaLauncherSubscribedPodcast.coverUrl get() = "${Settings.SERVER_STATIC_URL}/discover/images/webp/960/$id.webp"

    private val NovaLauncherSubscribedPodcast.intent get() = context.launcherIntent
        .setAction(Settings.INTENT_OPEN_APP_PODCAST_UUID)
        .putExtra(Settings.PODCAST_UUID, id)
        .putExtra(Settings.SOURCE_VIEW, SourceView.NOVA_LAUNCHER_SUBSCRIBED_PODCASTS.analyticsValue)

    private val NovaLauncherTrendingPodcast.coverUrl get() = "${Settings.SERVER_STATIC_URL}/discover/images/webp/960/$id.webp"

    private val NovaLauncherTrendingPodcast.intent get() = context.launcherIntent
        .setAction(Settings.INTENT_OPEN_APP_PODCAST_UUID)
        .putExtra(Settings.PODCAST_UUID, id)
        .putExtra(Settings.SOURCE_VIEW, SourceView.NOVA_LAUNCHER_TRENDING_PODCASTS.analyticsValue)

    private val Context.launcherIntent get() = requireNotNull(packageManager.getLaunchIntentForPackage(packageName)) {
        "Missing launcher intent for $packageName"
    }

    private fun ApplePodcastCategory.Companion.fromCategories(categories: String) = categories.split('\n').mapNotNull(ApplePodcastCategory::valueOfSafe)
}
