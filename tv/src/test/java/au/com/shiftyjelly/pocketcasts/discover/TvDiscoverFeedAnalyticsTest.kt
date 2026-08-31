package au.com.shiftyjelly.pocketcasts.discover

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
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
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class TvDiscoverFeedAnalyticsTest {

    private val eventHorizon = mock<EventHorizon>()
    private val discoverCountryCode = mock<UserSetting<String>> {
        whenever(it.value).thenReturn("us")
    }
    private val settings = mock<Settings> {
        whenever(it.discoverCountryCode).thenReturn(discoverCountryCode)
    }

    private fun analytics(source: String = "home", localRowIds: Set<String> = emptySet()) = TvDiscoverFeedAnalytics(eventHorizon, settings, source, localRowIds)

    @Test
    fun `list impression is tracked with the row id and source`() {
        analytics(source = "search").trackListImpression(podcastsRow(id = "trending"))

        verify(eventHorizon).track(DiscoverListImpressionEvent(listId = "trending", source = "search"))
    }

    @Test
    fun `list impression is not tracked for a local row`() {
        analytics(localRowIds = setOf("trending")).trackListImpression(podcastsRow(id = "trending"))

        verifyNoInteractions(eventHorizon)
    }

    @Test
    fun `list impression is not tracked for a row without a list id`() {
        analytics().trackListImpression(TvDiscoverRow.Podcasts(id = "trending", title = "Trending", podcasts = emptyList(), listId = null))

        verifyNoInteractions(eventHorizon)
    }

    @Test
    fun `list impression is not tracked for a banner row`() {
        analytics().trackListImpression(
            TvDiscoverRow.Banner(id = "create_account", title = "", banner = TvDiscoverBanner.CreateAccount),
        )

        verifyNoInteractions(eventHorizon)
    }

    @Test
    fun `list impression is not tracked for a categories row`() {
        analytics().trackListImpression(
            TvDiscoverRow.Categories(id = "categories", title = "Browse", categories = emptyList()),
        )

        verifyNoInteractions(eventHorizon)
    }

    @Test
    fun `podcast tapped is tracked with the list id, podcast uuid and source`() {
        analytics(source = "search").trackPodcastTapped(podcastsRow(id = "trending"), podcast("podcast-1"))

        verify(eventHorizon).track(
            DiscoverListPodcastTappedEvent(listId = "trending", podcastUuid = "podcast-1", source = "search"),
        )
        verify(eventHorizon, never()).track(any<DiscoverFeaturedPodcastTappedEvent>())
    }

    @Test
    fun `podcast tapped carries the list datetime when the section provides it`() {
        val row = TvDiscoverRow.Podcasts(id = "trending", title = "Trending", podcasts = emptyList(), listId = "trending", listDatetime = "2026-08-27")

        analytics(source = "search").trackPodcastTapped(row, podcast("podcast-1"))

        verify(eventHorizon).track(
            DiscoverListPodcastTappedEvent(listId = "trending", podcastUuid = "podcast-1", listDatetime = "2026-08-27", source = "search"),
        )
    }

    @Test
    fun `a featured podcast tap also tracks the featured podcast event`() {
        val row = TvDiscoverRow.FeaturedPodcasts(id = "featured", title = "Featured", podcasts = emptyList(), listId = "featured")

        analytics().trackPodcastTapped(row, podcast("podcast-1"))

        verify(eventHorizon).track(DiscoverListPodcastTappedEvent(listId = "featured", podcastUuid = "podcast-1", source = "home"))
        verify(eventHorizon).track(DiscoverFeaturedPodcastTappedEvent(podcastUuid = "podcast-1"))
    }

    @Test
    fun `a sponsored podcast tap also tracks an ad category event with the unknown name`() {
        analytics().trackPodcastTapped(podcastsRow(id = "trending"), podcast("podcast-1", isSponsored = true))

        verify(eventHorizon).track(DiscoverListPodcastTappedEvent(listId = "trending", podcastUuid = "podcast-1", source = "home"))
        verify(eventHorizon).track(
            DiscoverAdCategoryTappedEvent(name = "unknown", region = "us", id = 0, podcastId = "podcast-1"),
        )
    }

    @Test
    fun `podcast tapped is not tracked for a local row`() {
        analytics(localRowIds = setOf("trending")).trackPodcastTapped(podcastsRow(id = "trending"), podcast("podcast-1"))

        verifyNoInteractions(eventHorizon)
    }

    @Test
    fun `episode played tracks both the tapped and the play events`() {
        analytics(source = "search").trackEpisodePlayed(podcastsRow(id = "trending"), episode())

        verify(eventHorizon).track(
            DiscoverListEpisodeTappedEvent(listId = "trending", podcastUuid = "podcast-1", episodeUuid = "episode-1", source = "search"),
        )
        verify(eventHorizon).track(DiscoverListEpisodePlayEvent(listId = "trending", podcastUuid = "podcast-1"))
    }

    @Test
    fun `episode podcast tapped tracks the podcast tapped event`() {
        analytics().trackEpisodePodcastTapped(podcastsRow(id = "trending"), episode())

        verify(eventHorizon).track(
            DiscoverListPodcastTappedEvent(listId = "trending", podcastUuid = "podcast-1", source = "home"),
        )
    }

    @Test
    fun `category podcast tap tracks the list podcast event when a list id is present`() {
        analytics().trackCategoryPodcastTapped(openedCategory(), listId = "list-1", podcast("podcast-1"))

        verify(eventHorizon).track(
            DiscoverListPodcastTappedEvent(listId = "list-1", podcastUuid = "podcast-1", source = "home"),
        )
        verify(eventHorizon, never()).track(any<DiscoverAdCategoryTappedEvent>())
    }

    @Test
    fun `category podcast tap without a list id tracks nothing for a non-sponsored podcast`() {
        analytics().trackCategoryPodcastTapped(openedCategory(), listId = null, podcast("podcast-1"))

        verifyNoInteractions(eventHorizon)
    }

    @Test
    fun `a sponsored category podcast tap tracks an ad category event`() {
        analytics().trackCategoryPodcastTapped(openedCategory(id = 7, name = "News"), listId = null, podcast("podcast-1", isSponsored = true))

        verify(eventHorizon).track(
            DiscoverAdCategoryTappedEvent(name = "News", region = "us", id = 7, podcastId = "podcast-1"),
        )
    }

    @Test
    fun `category pill tap tracks the pill event with the region, index and visit count`() {
        val category = DiscoverCategory(id = 3, name = "Comedy", icon = "", source = "", totalVisits = 42, isSponsored = true)

        analytics(source = "search").trackCategoryPillTapped(category, index = 2)

        verify(eventHorizon).track(
            DiscoverCategoriesPillTappedEvent(name = "Comedy", region = "us", index = 2, visits = 42, sponsored = true, source = "search"),
        )
    }

    @Test
    fun `banner tap tracks the banner row event with the banner id`() {
        analytics().trackBannerTapped(TvDiscoverBanner.DiscoverMore)

        verify(eventHorizon).track(BannerRowTappedEvent(type = "discover_more"))
    }

    private fun podcastsRow(id: String) = TvDiscoverRow.Podcasts(id = id, title = "Trending", podcasts = emptyList(), listId = id)

    private fun podcast(uuid: String, isSponsored: Boolean = false) = TvDiscoverPodcast(
        uuid = uuid,
        title = "Podcast $uuid",
        author = "Author",
        description = "Description",
        isSponsored = isSponsored,
    )

    private fun episode() = TvDiscoverEpisode(
        episodeUuid = "episode-1",
        episodeTitle = "Episode",
        podcastUuid = "podcast-1",
        podcastTitle = "Podcast",
    )

    private fun openedCategory(id: Int = 1, name: String = "Category") = TvOpenedCategory(id = id, name = name, source = "home")
}
