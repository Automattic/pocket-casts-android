package au.com.shiftyjelly.pocketcasts.discover

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import com.automattic.eventhorizon.BannerRowTappedEvent
import com.automattic.eventhorizon.DiscoverAdCategoryTappedEvent
import com.automattic.eventhorizon.DiscoverCategoriesPillTappedEvent
import com.automattic.eventhorizon.DiscoverFeaturedPodcastTappedEvent
import com.automattic.eventhorizon.DiscoverListEpisodePlayEvent
import com.automattic.eventhorizon.DiscoverListEpisodeTappedEvent
import com.automattic.eventhorizon.DiscoverListImpressionEvent
import com.automattic.eventhorizon.DiscoverListPodcastTappedEvent
import com.automattic.eventhorizon.EventHorizon

class TvDiscoverFeedAnalytics(
    private val eventHorizon: EventHorizon,
    private val settings: Settings,
    private val source: String,
    private val localRowIds: Set<String> = emptySet(),
) {
    fun trackListImpression(row: TvDiscoverRow) {
        val listId = row.discoverListId() ?: return
        eventHorizon.track(DiscoverListImpressionEvent(listId = listId, source = source))
    }

    fun trackPodcastTapped(row: TvDiscoverRow, podcast: TvDiscoverPodcast) {
        val listId = row.discoverListId() ?: return
        eventHorizon.track(DiscoverListPodcastTappedEvent(listId = listId, podcastUuid = podcast.uuid, source = source))
        if (row is TvDiscoverRow.FeaturedPodcasts) {
            eventHorizon.track(DiscoverFeaturedPodcastTappedEvent(podcastUuid = podcast.uuid))
        } else if (podcast.isSponsored) {
            trackSponsoredPodcastTapped(podcast.uuid)
        }
    }

    fun trackEpisodePlayed(row: TvDiscoverRow, episode: TvDiscoverEpisode) {
        val listId = row.discoverListId() ?: return
        eventHorizon.track(
            DiscoverListEpisodeTappedEvent(listId = listId, podcastUuid = episode.podcastUuid, episodeUuid = episode.episodeUuid, source = source),
        )
        eventHorizon.track(DiscoverListEpisodePlayEvent(listId = listId, podcastUuid = episode.podcastUuid))
    }

    fun trackEpisodePodcastTapped(row: TvDiscoverRow, episode: TvDiscoverEpisode) {
        val listId = row.discoverListId() ?: return
        eventHorizon.track(DiscoverListPodcastTappedEvent(listId = listId, podcastUuid = episode.podcastUuid, source = source))
    }

    fun trackCategoryPodcastTapped(category: TvOpenedCategory, listId: String?, podcast: TvDiscoverPodcast) {
        if (listId != null) {
            eventHorizon.track(DiscoverListPodcastTappedEvent(listId = listId, podcastUuid = podcast.uuid, source = source))
        }
        if (podcast.isSponsored) {
            eventHorizon.track(
                DiscoverAdCategoryTappedEvent(name = category.name, region = discoverRegion(), id = category.id.toLong(), podcastId = podcast.uuid),
            )
        }
    }

    fun trackCategoryPillTapped(category: DiscoverCategory, index: Int) {
        eventHorizon.track(
            DiscoverCategoriesPillTappedEvent(
                name = category.name,
                region = discoverRegion(),
                index = index.toLong(),
                visits = category.totalVisits.toLong(),
                sponsored = category.isSponsored ?: false,
                source = source,
            ),
        )
    }

    fun trackBannerTapped(banner: TvDiscoverBanner) {
        eventHorizon.track(BannerRowTappedEvent(type = banner.id))
    }

    private fun trackSponsoredPodcastTapped(podcastUuid: String) {
        eventHorizon.track(
            DiscoverAdCategoryTappedEvent(name = UNKNOWN_VALUE, region = discoverRegion(), id = 0, podcastId = podcastUuid),
        )
    }

    private fun discoverRegion(): String = settings.discoverCountryCode.value

    private fun TvDiscoverRow.discoverListId(): String? = when (this) {
        is TvDiscoverRow.FeaturedPodcasts,
        is TvDiscoverRow.SinglePodcast,
        is TvDiscoverRow.Podcasts,
        is TvDiscoverRow.Episodes,
        -> id.takeUnless { it in localRowIds }

        is TvDiscoverRow.Banner,
        is TvDiscoverRow.Categories,
        -> null
    }

    companion object {
        private const val UNKNOWN_VALUE = "unknown"
    }
}
