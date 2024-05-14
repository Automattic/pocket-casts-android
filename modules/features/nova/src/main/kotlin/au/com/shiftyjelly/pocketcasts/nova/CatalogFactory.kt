package au.com.shiftyjelly.pocketcasts.nova

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherSubscribedPodcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import io.branch.engage.conduit.source.Catalog
import io.branch.engage.conduit.source.CatalogItem
import io.branch.engage.conduit.source.TypeData
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal class CatalogFactory(
    private val context: Context,
) {
    fun subscribedPodcasts(data: List<NovaLauncherSubscribedPodcast>) = Catalog(
        id = "SubscribedPodcasts",
        label = context.getString(LR.string.nova_launcher_subscribed_podcasts),
        items = data.map { podcast ->
            CatalogItem.Base(
                id = podcast.id,
                intent = podcast.intent,
                lastUsedTimestamp = podcast.lastUsedTimestamp,
                typeData = TypeData.Podcast(
                    name = podcast.title,
                    iconUrl = podcast.coverUrl,
                    originalReleaseTimestamp = podcast.initialReleaseTimestamp,
                    latestReleaseTimestamp = podcast.latestReleaseTimestamp,
                ),
            )
        },
    )

    private val NovaLauncherSubscribedPodcast.coverUrl get() = "${Settings.SERVER_STATIC_URL}/discover/images/webp/960/$id.webp"

    private val NovaLauncherSubscribedPodcast.intent get() = context.launcherIntent
        .setAction(Settings.INTENT_OPEN_APP_PODCAST_UUID)
        .putExtra(Settings.PODCAST_UUID, id)
        .putExtra(Settings.SOURCE_VIEW, SourceView.NOVA_LAUNCHER.analyticsValue)

    private val Context.launcherIntent get() = requireNotNull(packageManager.getLaunchIntentForPackage(packageName)) {
        "Missing launcher intent for $packageName"
    }
}
