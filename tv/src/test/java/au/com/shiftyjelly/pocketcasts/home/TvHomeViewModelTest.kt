package au.com.shiftyjelly.pocketcasts.home

import android.content.Context
import android.content.res.Resources
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverBanner
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverEpisode
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverFeedLoader
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverPodcast
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverRow
import au.com.shiftyjelly.pocketcasts.discover.TvOpenedCategory
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.UpNextDao
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.model.Discover
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverEpisode
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRegion
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRow
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ExpandedStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.servers.model.SponsoredPodcast
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.eventhorizon.BannerRowTappedEvent
import com.automattic.eventhorizon.DiscoverAdCategoryTappedEvent
import com.automattic.eventhorizon.DiscoverCategoriesPillTappedEvent
import com.automattic.eventhorizon.DiscoverFeaturedPodcastTappedEvent
import com.automattic.eventhorizon.DiscoverListEpisodePlayEvent
import com.automattic.eventhorizon.DiscoverListEpisodeTappedEvent
import com.automattic.eventhorizon.DiscoverListImpressionEvent
import com.automattic.eventhorizon.DiscoverListPodcastTappedEvent
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.HomeShownEvent
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Single
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalCoroutinesApi::class)
class TvHomeViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val listRepository = mock<ListRepository>()
    private val syncManager = mock<SyncManager> {
        on { isLoggedInObservable } doReturn BehaviorRelay.createDefault(false)
    }
    private val podcastDao = mock<PodcastDao> {
        on { findAllIn(any()) }.thenReturn(emptyList())
    }
    private val upNextDao = mock<UpNextDao> {
        on { getUpNextBaseEpisodes(any()) }.thenReturn(emptyList())
    }
    private val playlistManager = mock<PlaylistManager> {
        whenever(it.smartEpisodesFlow(any(), any(), anyOrNull(), any())).thenReturn(flowOf(emptyList()))
    }
    private val resources = mock<Resources>()
    private val context = mock<Context> {
        whenever(it.resources).thenReturn(resources)
        whenever(it.getString(LR.string.tv_home_keep_listening)).thenReturn("Keep Listening")
        whenever(it.getString(LR.string.up_next)).thenReturn("Up Next")
        whenever(it.getString(LR.string.filters_title_new_releases)).thenReturn("New Releases")
        whenever(it.getString(LR.string.tv_search_browse_categories)).thenReturn("Browse categories")
        whenever(it.getString(LR.string.tv_sponsored_podcast_section_title)).thenReturn("Pocket Casts recommends")
    }
    private val discoverCountryCode = mock<UserSetting<String>> {
        whenever(it.value).thenReturn("us")
    }
    private val settings = mock<Settings> {
        whenever(it.discoverCountryCode).thenReturn(discoverCountryCode)
    }
    private val episodeManager = mock<EpisodeManager>()
    private val podcastManager = mock<PodcastManager>()
    private val playbackManager = mock<PlaybackManager>()
    private val eventHorizon = mock<EventHorizon>()

    @Test
    fun `all rows load in feed order`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(
            discover(
                row(
                    id = "featured",
                    title = "Row Featured",
                    source = "https://lists/featured.json",
                    displayStyle = DisplayStyle.Carousel(),
                ),
                row(
                    id = "sponsored-id",
                    title = "Row Sponsored",
                    source = "https://lists/sponsored.json",
                    displayStyle = DisplayStyle.SinglePodcast(),
                ),
                row(
                    id = "videos-id",
                    title = "Row Videos",
                    source = "https://lists/videos.json",
                    type = ListType.EpisodeList,
                ),
                row(
                    id = "trending",
                    title = "Row Trending",
                    source = "https://lists/trending.json",
                    displayStyle = DisplayStyle.SmallList(),
                ),
                row(
                    id = "curated-id",
                    title = "Row Curated",
                    source = "https://lists/curated.json",
                    displayStyle = DisplayStyle.LargeList(),
                ),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://lists/featured.json"), any()))
            .thenReturn(podcastFeed("podcast-featured"))
        whenever(listRepository.getListFeed(eq("https://lists/sponsored.json"), any()))
            .thenReturn(podcastFeed("podcast-sponsored"))
        whenever(listRepository.getListFeed(eq("https://lists/videos.json"), any()))
            .thenReturn(episodeFeed("episode-1"))
        whenever(listRepository.getListFeed(eq("https://lists/trending.json"), any()))
            .thenReturn(podcastFeed("podcast-trending"))
        whenever(listRepository.getListFeed(eq("https://lists/curated.json"), any()))
            .thenReturn(podcastFeed("podcast-curated"))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem() as TvHomeUiState.Ready
            assertEquals(
                listOf("featured", "sponsored-id", "videos-id", "trending", "curated-id"),
                state.rows.map { it.id },
            )
            assertTrue(state.rows[0] is TvDiscoverRow.FeaturedPodcasts)
            assertTrue(state.rows[1] is TvDiscoverRow.SinglePodcast)
            assertTrue(state.rows[2] is TvDiscoverRow.Episodes)
            assertTrue(state.rows[3] is TvDiscoverRow.Podcasts)
            assertTrue(state.rows[4] is TvDiscoverRow.Podcasts)
        }
    }

    @Test
    fun `authenticated rows are excluded when signed out`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(
            discover(
                row(
                    id = "recommendations_user",
                    title = "Row For You",
                    source = "https://lists/user.json",
                    authenticated = true,
                ),
                row(id = "trending", title = "Row Trending", source = "https://lists/trending.json"),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://lists/trending.json"), any()))
            .thenReturn(podcastFeed("podcast-trending"))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem() as TvHomeUiState.Ready
            assertEquals(listOf("trending"), state.rows.map { it.id })
        }
        verify(listRepository, never()).getListFeed(eq("https://lists/user.json"), any())
    }

    @Test
    fun `rows without a source are dropped`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(
            discover(
                row(id = "up-next-placeholder", title = "Up Next", source = ""),
                row(id = "trending", title = "Row Trending", source = "https://lists/trending.json"),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://lists/trending.json"), any()))
            .thenReturn(podcastFeed("podcast-trending"))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem() as TvHomeUiState.Ready
            assertEquals(listOf("trending"), state.rows.map { it.id })
        }
        verify(listRepository, never()).getListFeed(eq(""), any())
    }

    @Test
    fun `authenticated rows load when signed in`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(true)
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = true)).thenReturn(
            discover(
                row(
                    id = "recommendations_user",
                    title = "Row For You",
                    source = "https://lists/user.json",
                    authenticated = true,
                ),
                row(id = "trending", title = "Row Trending", source = "https://lists/trending.json"),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://lists/user.json"), eq(true)))
            .thenReturn(podcastFeed("podcast-user"))
        whenever(listRepository.getListFeed(eq("https://lists/trending.json"), any()))
            .thenReturn(podcastFeed("podcast-trending"))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem() as TvHomeUiState.Ready
            assertEquals(listOf("recommendations_user", "trending"), state.rows.map { it.id })
        }
    }

    @Test
    fun `sponsored row marks its podcasts as sponsored`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(
            discover(
                row(
                    id = "sponsored-id",
                    title = "Row Sponsored",
                    source = "https://lists/sponsored.json",
                    displayStyle = DisplayStyle.SinglePodcast(),
                    sponsored = true,
                ),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://lists/sponsored.json"), any()))
            .thenReturn(podcastFeed("podcast-sponsored"))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem() as TvHomeUiState.Ready
            val row = state.rows.single() as TvDiscoverRow.SinglePodcast
            assertTrue(row.podcasts.single().isSponsored)
        }
    }

    @Test
    fun `rows that fail to load are dropped`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(
            discover(
                row(id = "featured", title = "Row Featured", source = "https://lists/featured.json"),
                row(id = "empty-id", title = "Row Empty", source = "https://lists/empty.json"),
                row(id = "trending", title = "Row Trending", source = "https://lists/trending.json"),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://lists/featured.json"), any())).thenReturn(null)
        whenever(listRepository.getListFeed(eq("https://lists/empty.json"), any())).thenReturn(podcastFeed())
        whenever(listRepository.getListFeed(eq("https://lists/trending.json"), any()))
            .thenReturn(podcastFeed("podcast-trending"))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem() as TvHomeUiState.Ready
            assertEquals(listOf("trending"), state.rows.map { it.id })
        }
    }

    @Test
    fun `category sponsor rows are excluded`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(
            discover(
                row(
                    id = "category-ad",
                    title = "Row Category Ad",
                    source = "https://lists/category-ad.json",
                    sponsored = true,
                    categoryId = 3,
                ),
                row(id = "trending", title = "Row Trending", source = "https://lists/trending.json"),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://lists/trending.json"), any()))
            .thenReturn(podcastFeed("podcast-trending"))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem() as TvHomeUiState.Ready
            assertEquals(listOf("trending"), state.rows.map { it.id })
        }
        verify(listRepository, never()).getListFeed(eq("https://lists/category-ad.json"), any())
    }

    @Test
    fun `banner rows are parsed from the feed even without a source`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(
            discover(
                bannerRow(id = "create_account"),
                bannerRow(id = "discover_more"),
                row(id = "trending", title = "Row Trending", source = "https://lists/trending.json"),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://lists/trending.json"), any()))
            .thenReturn(podcastFeed("podcast-trending"))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem() as TvHomeUiState.Ready
            assertEquals(listOf("create_account", "discover_more", "trending"), state.rows.map { it.id })
            assertEquals(TvDiscoverBanner.CreateAccount, (state.rows[0] as TvDiscoverRow.Banner).banner)
            assertEquals(TvDiscoverBanner.DiscoverMore, (state.rows[1] as TvDiscoverRow.Banner).banner)
        }
    }

    @Test
    fun `banner rows with an unknown id are dropped`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(
            discover(
                bannerRow(id = "mystery_banner"),
                row(id = "trending", title = "Row Trending", source = "https://lists/trending.json"),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://lists/trending.json"), any()))
            .thenReturn(podcastFeed("podcast-trending"))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem() as TvHomeUiState.Ready
            assertEquals(listOf("trending"), state.rows.map { it.id })
        }
    }

    @Test
    fun `category rows load their pills with region resolved sources`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(
            discover(
                row(
                    id = "categories",
                    title = "Row Categories",
                    source = "https://lists/categories.json",
                    type = ListType.Categories,
                    displayStyle = DisplayStyle.Pills(),
                ),
                row(id = "trending", title = "Row Trending", source = "https://lists/trending.json"),
            ),
        )
        whenever(listRepository.getCategoriesList("https://lists/categories.json")).thenReturn(
            listOf(
                DiscoverCategory(id = 1, name = "Comedy", icon = "", source = "https://category/[regionCode].json"),
                DiscoverCategory(id = 2, name = "True Crime", icon = "", source = "https://category/true-crime.json"),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://lists/trending.json"), any()))
            .thenReturn(podcastFeed("podcast-trending"))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem() as TvHomeUiState.Ready
            assertEquals(listOf("categories", "trending"), state.rows.map { it.id })
            val categoriesRow = state.rows[0] as TvDiscoverRow.Categories
            assertEquals("Browse categories", categoriesRow.title)
            assertEquals(listOf("Comedy", "True Crime"), categoriesRow.categories.map { it.name })
            assertEquals("https://category/us.json", categoriesRow.categories[0].source)
        }
    }

    @Test
    fun `category rows are dropped when their pills fail to load`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(
            discover(
                row(
                    id = "categories",
                    title = "Row Categories",
                    source = "https://lists/categories.json",
                    type = ListType.Categories,
                    displayStyle = DisplayStyle.Pills(),
                ),
                row(id = "trending", title = "Row Trending", source = "https://lists/trending.json"),
            ),
        )
        whenever(listRepository.getCategoriesList("https://lists/categories.json"))
            .thenThrow(RuntimeException("boom"))
        whenever(listRepository.getListFeed(eq("https://lists/trending.json"), any()))
            .thenReturn(podcastFeed("podcast-trending"))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem() as TvHomeUiState.Ready
            assertEquals(listOf("trending"), state.rows.map { it.id })
        }
    }

    @Test
    fun `categoryPodcasts maps the list feed podcasts`() = runTest {
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(discover())
        whenever(listRepository.getListFeed(eq("https://category/us.json"), any()))
            .thenReturn(podcastFeed("podcast-1", "podcast-2"))

        val podcasts = createViewModel().categoryPodcasts(categoryId = 7, source = "https://category/us.json").podcasts

        assertEquals(listOf("podcast-1", "podcast-2"), podcasts.map { it.uuid })
    }

    @Test
    fun `categoryPodcasts throws when the feed cannot be loaded`() = runTest {
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(discover())
        whenever(listRepository.getListFeed(eq("https://category/us.json"), any())).thenReturn(null)

        val result = runCatching { createViewModel().categoryPodcasts(categoryId = 7, source = "https://category/us.json") }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `categoryPodcasts merges sponsored ads at a fixed position`() = runTest {
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(
            discover(
                row(
                    id = "category-ad",
                    title = "Row Category Ad",
                    source = "https://lists/category-ad.json",
                    sponsored = true,
                    categoryId = 7,
                ),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://category/us.json"), any()))
            .thenReturn(podcastFeed("p-0", "p-1", "p-2", "p-3", "p-4", "p-5", "p-6"))
        whenever(listRepository.getListFeed(eq("https://lists/category-ad.json"), any()))
            .thenReturn(podcastFeed("ad-1"))

        val podcasts = createViewModel().categoryPodcasts(categoryId = 7, source = "https://category/us.json").podcasts

        assertEquals("ad-1", podcasts[5].uuid)
        assertTrue(podcasts[5].isSponsored)
        assertEquals(8, podcasts.size)
    }

    @Test
    fun `categoryPodcasts appends the sponsored ad when the category is short`() = runTest {
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(
            discover(
                row(
                    id = "category-ad",
                    title = "Row Category Ad",
                    source = "https://lists/category-ad.json",
                    sponsored = true,
                    categoryId = 7,
                ),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://category/us.json"), any()))
            .thenReturn(podcastFeed("p-0", "p-1"))
        whenever(listRepository.getListFeed(eq("https://lists/category-ad.json"), any()))
            .thenReturn(podcastFeed("ad-1"))

        val podcasts = createViewModel().categoryPodcasts(categoryId = 7, source = "https://category/us.json").podcasts

        assertEquals(listOf("p-0", "p-1", "ad-1"), podcasts.map { it.uuid })
        assertTrue(podcasts.last().isSponsored)
    }

    @Test
    fun `sponsored single-podcast row uses the recommends title`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(
            discover(
                row(
                    id = "sponsored-id",
                    title = "Row Sponsored",
                    source = "https://lists/sponsored.json",
                    displayStyle = DisplayStyle.SinglePodcast(),
                    sponsored = true,
                ),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://lists/sponsored.json"), any()))
            .thenReturn(podcastFeed("podcast-sponsored"))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem() as TvHomeUiState.Ready
            assertEquals("Pocket Casts recommends", state.rows.single().title)
        }
    }

    @Test
    fun `sponsored podcasts are injected at their feed positions`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(
            discover(
                row(
                    id = "trending",
                    title = "Row Trending",
                    source = "https://lists/trending.json",
                    sponsoredPodcasts = listOf(SponsoredPodcast(position = 1, source = "https://lists/ad.json")),
                ),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://lists/trending.json"), any()))
            .thenReturn(podcastFeed("p-0", "p-1", "p-2"))
        whenever(listRepository.getListFeed(eq("https://lists/ad.json"), any()))
            .thenReturn(podcastFeed("ad-1"))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem() as TvHomeUiState.Ready
            val row = state.rows.single() as TvDiscoverRow.Podcasts
            assertEquals(listOf("p-0", "ad-1", "p-1", "p-2"), row.podcasts.map { it.uuid })
            assertTrue(row.podcasts[1].isSponsored)
        }
    }

    @Test
    fun `sponsored podcast already present in the row is not duplicated`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(
            discover(
                row(
                    id = "trending",
                    title = "Row Trending",
                    source = "https://lists/trending.json",
                    sponsoredPodcasts = listOf(SponsoredPodcast(position = 0, source = "https://lists/ad.json")),
                ),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://lists/trending.json"), any()))
            .thenReturn(podcastFeed("p-0", "ad-1", "p-2"))
        whenever(listRepository.getListFeed(eq("https://lists/ad.json"), any()))
            .thenReturn(podcastFeed("ad-1"))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem() as TvHomeUiState.Ready
            val row = state.rows.single() as TvDiscoverRow.Podcasts
            assertEquals(listOf("ad-1", "p-0", "p-2"), row.podcasts.map { it.uuid })
            assertEquals(1, row.podcasts.count { it.uuid == "ad-1" })
            assertTrue(row.podcasts.first().isSponsored)
        }
    }

    @Test
    fun `rows with duplicate ids are deduplicated`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(
            discover(
                row(id = "trending", title = "Row Trending", source = "https://lists/trending.json"),
                row(id = "trending", title = "Row Trending Again", source = "https://lists/trending-2.json"),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://lists/trending.json"), any()))
            .thenReturn(podcastFeed("podcast-1"))
        whenever(listRepository.getListFeed(eq("https://lists/trending-2.json"), any()))
            .thenReturn(podcastFeed("podcast-2"))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem() as TvHomeUiState.Ready
            assertEquals(listOf("trending"), state.rows.map { it.id })
        }
    }

    @Test
    fun `rows not available in the current region are excluded`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(
            discover(
                row(id = "featured", title = "Row Featured", source = "https://lists/featured.json"),
                row(
                    id = "trending",
                    title = "Row Trending",
                    source = "https://lists/trending.json",
                    regions = listOf("au"),
                ),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://lists/featured.json"), any()))
            .thenReturn(podcastFeed("podcast-featured"))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem() as TvHomeUiState.Ready
            assertEquals(listOf("featured"), state.rows.map { it.id })
        }
        verify(listRepository, never()).getListFeed(eq("https://lists/trending.json"), any())
    }

    @Test
    fun `list feed title is preferred over row title`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(
            discover(
                row(id = "featured", title = "Row Featured", source = "https://lists/featured.json"),
                row(id = "trending", title = "Row Trending", source = "https://lists/trending.json"),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://lists/featured.json"), any()))
            .thenReturn(podcastFeed("podcast-featured", title = "Feed Featured"))
        whenever(listRepository.getListFeed(eq("https://lists/trending.json"), any()))
            .thenReturn(podcastFeed("podcast-trending", title = null))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem() as TvHomeUiState.Ready
            assertEquals(listOf("Feed Featured", "Row Trending"), state.rows.map { it.title })
        }
    }

    @Test
    fun `feed subtitle is prefixed to the feed title`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(
            discover(row(id = "rnz", title = "Row RNZ", source = "https://lists/rnz.json")),
        )
        whenever(listRepository.getListFeed(eq("https://lists/rnz.json"), any()))
            .thenReturn(podcastFeed("podcast-rnz", title = "RNZ Podcasts", subtitle = "Network Highlight"))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem() as TvHomeUiState.Ready
            assertEquals("Network Highlight: RNZ Podcasts", state.rows.single().title)
        }
    }

    @Test
    fun `feed title and subtitle are localised`() = runTest {
        whenever(resources.getString(LR.string.discover_featured)).thenReturn("Localised featured")
        whenever(resources.getString(LR.string.discover_trending)).thenReturn("Localised trending")
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(
            discover(row(id = "featured", title = "Row Featured", source = "https://lists/featured.json")),
        )
        whenever(listRepository.getListFeed(eq("https://lists/featured.json"), any()))
            .thenReturn(podcastFeed("podcast-featured", title = "Featured", subtitle = "Trending"))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem() as TvHomeUiState.Ready
            assertEquals("Localised trending: Localised featured", state.rows.single().title)
        }
    }

    @Test
    fun `feed failure shows error state and retry reloads`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false))
            .thenThrow(RuntimeException("Network error"))
            .thenReturn(discover(row(id = "trending", title = "Row Trending", source = "https://lists/trending.json")))
        whenever(listRepository.getListFeed(eq("https://lists/trending.json"), any()))
            .thenReturn(podcastFeed("podcast-trending"))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvHomeUiState.Error, awaitItem())

            viewModel.load()

            assertEquals(TvHomeUiState.Loading, awaitItem())
            val state = awaitItem() as TvHomeUiState.Ready
            assertEquals(listOf("trending"), state.rows.map { it.id })
        }
    }

    @Test
    fun `feed failure keeps local rows on screen`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenThrow(RuntimeException("Network error"))
        whenever(upNextDao.getUpNextBaseEpisodes(any())).thenReturn(
            listOf(episode(uuid = "episode-1", podcastUuid = "podcast-1")),
        )
        whenever(podcastDao.findAllIn(any()))
            .thenReturn(listOf(Podcast(uuid = "podcast-1", title = "Podcast One")))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem() as TvHomeUiState.Ready
            assertEquals(listOf(TvHomeViewModel.KEEP_LISTENING_ROW_ID), state.rows.map { it.id })
        }
    }

    @Test
    fun `keep listening row shows first up next episode even when signed out`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(discover())
        whenever(upNextDao.getUpNextBaseEpisodes(any())).thenReturn(
            listOf(
                episode(uuid = "episode-1", podcastUuid = "podcast-1"),
                episode(uuid = "episode-2", podcastUuid = "podcast-1"),
            ),
        )
        whenever(podcastDao.findAllIn(any()))
            .thenReturn(listOf(Podcast(uuid = "podcast-1", title = "Podcast One")))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem() as TvHomeUiState.Ready
            assertEquals(listOf(TvHomeViewModel.KEEP_LISTENING_ROW_ID), state.rows.map { it.id })
            val row = state.rows.single() as TvDiscoverRow.Episodes
            assertEquals("Keep Listening", row.title)
            val rowEpisode = row.episodes.single()
            assertEquals("episode-1", rowEpisode.episodeUuid)
            assertEquals("Podcast One", rowEpisode.podcastTitle)
        }
    }

    @Test
    fun `user episodes in up next are skipped`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(discover())
        whenever(upNextDao.getUpNextBaseEpisodes(any())).thenReturn(
            listOf(
                UserEpisode(uuid = "user-file", publishedDate = Date()),
                episode(uuid = "episode-1", podcastUuid = "podcast-1"),
            ),
        )
        whenever(podcastDao.findAllIn(any()))
            .thenReturn(listOf(Podcast(uuid = "podcast-1", title = "Podcast One")))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem() as TvHomeUiState.Ready
            val row = state.rows.single() as TvDiscoverRow.Episodes
            assertEquals(listOf("episode-1"), row.episodes.map { it.episodeUuid })
        }
    }

    @Test
    fun `signed in user sees keep listening, up next and new releases rows before discover rows`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(true)
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = true)).thenReturn(
            discover(row(id = "trending", title = "Row Trending", source = "https://lists/trending.json")),
        )
        whenever(listRepository.getListFeed(eq("https://lists/trending.json"), any()))
            .thenReturn(podcastFeed("podcast-trending"))
        whenever(upNextDao.getUpNextBaseEpisodes(any())).thenReturn(
            listOf(
                episode(uuid = "episode-1", podcastUuid = "podcast-1"),
                episode(uuid = "episode-2", podcastUuid = "podcast-1"),
                episode(uuid = "episode-3", podcastUuid = "podcast-2"),
            ),
        )
        whenever(playlistManager.smartEpisodesFlow(any(), any(), anyOrNull(), any())).thenReturn(
            flowOf(listOf(PlaylistEpisode.Available(episode(uuid = "episode-new", podcastUuid = "podcast-2")))),
        )
        whenever(podcastDao.findAllIn(any())).thenReturn(
            listOf(
                Podcast(uuid = "podcast-1", title = "Podcast One"),
                Podcast(uuid = "podcast-2", title = "Podcast Two"),
            ),
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem() as TvHomeUiState.Ready
            assertEquals(
                listOf(
                    TvHomeViewModel.KEEP_LISTENING_ROW_ID,
                    TvHomeViewModel.UP_NEXT_ROW_ID,
                    TvHomeViewModel.NEW_RELEASES_ROW_ID,
                    "trending",
                ),
                state.rows.map { it.id },
            )
            val upNextRow = state.rows[1] as TvDiscoverRow.Episodes
            assertEquals(listOf("episode-2", "episode-3"), upNextRow.episodes.map { it.episodeUuid })
            val newReleasesRow = state.rows[2] as TvDiscoverRow.Episodes
            assertEquals(listOf("episode-new"), newReleasesRow.episodes.map { it.episodeUuid })
            assertEquals("Podcast Two", newReleasesRow.episodes.single().podcastTitle)
        }
    }

    @Test
    fun `up next and new releases rows are hidden when signed out`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(discover())
        whenever(upNextDao.getUpNextBaseEpisodes(any())).thenReturn(
            listOf(
                episode(uuid = "episode-1", podcastUuid = "podcast-1"),
                episode(uuid = "episode-2", podcastUuid = "podcast-1"),
            ),
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem() as TvHomeUiState.Ready
            assertEquals(listOf(TvHomeViewModel.KEEP_LISTENING_ROW_ID), state.rows.map { it.id })
        }
    }

    @Test
    fun `playEpisode plays an episode that is already in the database`() = runTest {
        val episode = episode("episode-1", podcastUuid = "podcast-1")
        whenever(episodeManager.findByUuid("episode-1")).thenReturn(episode)

        createViewModel().playEpisode(homeEpisode())

        verifyBlocking(playbackManager) { playNowSuspend(episode = episode, sourceView = SourceView.DISCOVER) }
        verify(podcastManager, never()).findOrDownloadPodcastRxSingle(any(), any())
    }

    @Test
    fun `playEpisode fetches the podcast before playing an unknown episode`() = runTest {
        val episode = episode("episode-1", podcastUuid = "podcast-1")
        whenever(episodeManager.findByUuid("episode-1")).thenReturn(null, episode)
        whenever(podcastManager.findOrDownloadPodcastRxSingle("podcast-1")).thenReturn(Single.just(Podcast(uuid = "podcast-1")))

        createViewModel().playEpisode(homeEpisode())

        verifyBlocking(playbackManager) { playNowSuspend(episode = episode, sourceView = SourceView.DISCOVER) }
    }

    @Test
    fun `playEpisode reports when playback has started`() = runTest {
        val episode = episode("episode-1", podcastUuid = "podcast-1")
        whenever(episodeManager.findByUuid("episode-1")).thenReturn(episode)
        val viewModel = createViewModel()

        viewModel.playStarted.test {
            viewModel.playEpisode(homeEpisode())

            awaitItem()
        }
    }

    @Test
    fun `playEpisode reports a failure when the podcast fetch fails`() = runTest {
        whenever(episodeManager.findByUuid("episode-1")).thenReturn(null)
        whenever(podcastManager.findOrDownloadPodcastRxSingle("podcast-1")).thenReturn(Single.error(RuntimeException("boom")))
        val viewModel = createViewModel()

        viewModel.playFailures.test {
            viewModel.playEpisode(homeEpisode())

            awaitItem()
            verifyNoInteractions(playbackManager)
        }
    }

    private fun homeEpisode() = TvDiscoverEpisode(
        episodeUuid = "episode-1",
        episodeTitle = "Episode",
        podcastUuid = "podcast-1",
        podcastTitle = "Podcast",
    )

    @Test
    fun `tracks home shown`() = runTest {
        createViewModel().trackHomeShown()

        verify(eventHorizon).track(HomeShownEvent)
    }

    @Test
    fun `opening a featured podcast tracks both the list and featured events`() = runTest {
        val podcast = discoverPodcast("podcast-1")
        val row = TvDiscoverRow.FeaturedPodcasts(id = "list-featured", title = "Featured", podcasts = listOf(podcast))

        createViewModel().trackDiscoverPodcastTapped(row, podcast)

        verify(eventHorizon).track(DiscoverListPodcastTappedEvent(listId = "list-featured", podcastUuid = "podcast-1", source = "home"))
        verify(eventHorizon).track(DiscoverFeaturedPodcastTappedEvent(podcastUuid = "podcast-1"))
    }

    @Test
    fun `opening a sponsored podcast tracks the ad event`() = runTest {
        val podcast = discoverPodcast("podcast-1", isSponsored = true)
        val row = TvDiscoverRow.Podcasts(id = "list-trending", title = "Trending", podcasts = listOf(podcast))

        createViewModel().trackDiscoverPodcastTapped(row, podcast)

        verify(eventHorizon).track(DiscoverListPodcastTappedEvent(listId = "list-trending", podcastUuid = "podcast-1", source = "home"))
        verify(eventHorizon).track(DiscoverAdCategoryTappedEvent(name = "unknown", region = "us", id = 0, podcastId = "podcast-1"))
    }

    @Test
    fun `opening a sponsored featured podcast does not track the ad event`() = runTest {
        val podcast = discoverPodcast("podcast-1", isSponsored = true)
        val row = TvDiscoverRow.FeaturedPodcasts(id = "list-featured", title = "Featured", podcasts = listOf(podcast))

        createViewModel().trackDiscoverPodcastTapped(row, podcast)

        verify(eventHorizon).track(DiscoverFeaturedPodcastTappedEvent(podcastUuid = "podcast-1"))
        verify(eventHorizon, never()).track(any<DiscoverAdCategoryTappedEvent>())
    }

    @Test
    fun `opening a sponsored category podcast tracks the list and ad with the category details`() = runTest {
        val podcast = discoverPodcast("podcast-1", isSponsored = true)
        val category = TvOpenedCategory(id = 5, name = "True Crime", source = "https://category.json")

        createViewModel().trackCategoryPodcastTapped(category, listId = "list-tc", podcast = podcast)

        verify(eventHorizon).track(DiscoverListPodcastTappedEvent(listId = "list-tc", podcastUuid = "podcast-1", source = "home"))
        verify(eventHorizon).track(DiscoverAdCategoryTappedEvent(name = "True Crime", region = "us", id = 5, podcastId = "podcast-1"))
    }

    @Test
    fun `local rows do not track discover list events`() = runTest {
        val episode = TvDiscoverEpisode("episode-1", "Episode", "podcast-1", "Podcast")
        val row = TvDiscoverRow.Episodes(id = TvHomeViewModel.KEEP_LISTENING_ROW_ID, title = "Keep listening", episodes = listOf(episode))
        val viewModel = createViewModel()

        viewModel.trackDiscoverEpisodePlayed(row, episode)
        viewModel.trackDiscoverListShown(row)

        verifyNoInteractions(eventHorizon)
    }

    @Test
    fun `playing a discover episode tracks the tap and play events`() = runTest {
        val episode = TvDiscoverEpisode("episode-1", "Episode", "podcast-1", "Podcast")
        val row = TvDiscoverRow.Episodes(id = "list-videos", title = "Made for TV", episodes = listOf(episode))

        createViewModel().trackDiscoverEpisodePlayed(row, episode)

        verify(eventHorizon).track(DiscoverListEpisodeTappedEvent(listId = "list-videos", podcastUuid = "podcast-1", episodeUuid = "episode-1", source = "home"))
        verify(eventHorizon).track(DiscoverListEpisodePlayEvent(listId = "list-videos", podcastUuid = "podcast-1"))
    }

    @Test
    fun `showing a discover list tracks an impression`() = runTest {
        val row = TvDiscoverRow.Podcasts(id = "list-trending", title = "Trending", podcasts = listOf(discoverPodcast("podcast-1")))

        createViewModel().trackDiscoverListShown(row)

        verify(eventHorizon).track(DiscoverListImpressionEvent(listId = "list-trending", source = "home"))
    }

    @Test
    fun `opening a discover episode podcast tracks the list podcast tapped event`() = runTest {
        val episode = TvDiscoverEpisode("episode-1", "Episode", "podcast-1", "Podcast")
        val row = TvDiscoverRow.Episodes(id = "list-videos", title = "Made for TV", episodes = listOf(episode))

        createViewModel().trackDiscoverEpisodePodcastTapped(row, episode)

        verify(eventHorizon).track(DiscoverListPodcastTappedEvent(listId = "list-videos", podcastUuid = "podcast-1", source = "home"))
    }

    @Test
    fun `banner and categories rows do not track an impression`() = runTest {
        val viewModel = createViewModel()

        viewModel.trackDiscoverListShown(TvDiscoverRow.Banner(id = "banner", title = "", banner = TvDiscoverBanner.DiscoverMore))
        viewModel.trackDiscoverListShown(
            TvDiscoverRow.Categories(
                id = "categories",
                title = "Categories",
                categories = listOf(DiscoverCategory(id = 1, name = "Comedy", icon = "", source = "")),
            ),
        )

        verifyNoInteractions(eventHorizon)
    }

    @Test
    fun `tracks banner tapped`() = runTest {
        createViewModel().trackBannerTapped(TvDiscoverBanner.DiscoverMore)

        verify(eventHorizon).track(BannerRowTappedEvent(type = "discover_more"))
    }

    @Test
    fun `tracks category pill tapped`() = runTest {
        val category = DiscoverCategory(id = 3, name = "True Crime", icon = "", source = "https://category.json", totalVisits = 7, isSponsored = true)

        createViewModel().trackCategoryPillTapped(category, index = 2)

        verify(eventHorizon).track(
            DiscoverCategoriesPillTappedEvent(name = "True Crime", region = "us", index = 2, visits = 7, sponsored = true, source = "home"),
        )
    }

    private fun discoverPodcast(uuid: String, isSponsored: Boolean = false) = TvDiscoverPodcast(
        uuid = uuid,
        title = "Title",
        author = "Author",
        description = "Description",
        isSponsored = isSponsored,
    )

    private fun createViewModel() = TvHomeViewModel(
        discoverFeedLoader = TvDiscoverFeedLoader(
            listRepository = listRepository,
            settings = settings,
            applicationScope = CoroutineScope(coroutineRule.testDispatcher),
            context = context,
        ),
        playlistManager = playlistManager,
        podcastDao = podcastDao,
        upNextDao = upNextDao,
        syncManager = syncManager,
        episodeManager = episodeManager,
        podcastManager = podcastManager,
        playbackManager = playbackManager,
        eventHorizon = eventHorizon,
        settings = settings,
        context = context,
    )

    private fun episode(uuid: String, podcastUuid: String) = PodcastEpisode(
        uuid = uuid,
        podcastUuid = podcastUuid,
        title = "Episode $uuid",
        publishedDate = Date(),
    )

    private fun discover(vararg rows: DiscoverRow) = Discover(
        layout = rows.toList(),
        regions = mapOf("us" to DiscoverRegion(name = "United States", flag = "flag", code = "us")),
        regionCodeToken = "[regionCode]",
        regionNameToken = "[regionName]",
        defaultRegionCode = "us",
    )

    private fun row(
        title: String,
        source: String,
        id: String? = null,
        listUuid: String? = null,
        type: ListType = ListType.PodcastList,
        displayStyle: DisplayStyle = DisplayStyle.SmallList(),
        curated: Boolean = false,
        sponsored: Boolean = false,
        authenticated: Boolean = false,
        regions: List<String> = listOf("us"),
        categoryId: Int? = null,
        sponsoredPodcasts: List<SponsoredPodcast> = emptyList(),
    ) = DiscoverRow(
        id = id,
        type = type,
        displayStyle = displayStyle,
        expandedStyle = ExpandedStyle.PlainList(),
        expandedTopItemLabel = null,
        title = title,
        source = source,
        listUuid = listUuid,
        categoryId = categoryId,
        regions = regions,
        curated = curated,
        sponsored = sponsored,
        authenticated = authenticated,
        sponsoredPodcasts = sponsoredPodcasts,
        mostPopularCategoriesId = null,
        sponsoredCategoryIds = null,
    )

    private fun bannerRow(id: String) = row(
        id = id,
        title = "",
        source = "",
        type = ListType.Unknown("banner"),
        displayStyle = DisplayStyle.Unknown("inline_banner"),
    )

    private fun podcastFeed(vararg podcastUuids: String, title: String? = null, subtitle: String? = null) = listFeed(
        title = title,
        subtitle = subtitle,
        podcasts = podcastUuids.map { uuid ->
            DiscoverPodcast(
                uuid = uuid,
                title = "Podcast $uuid",
                url = null,
                author = null,
                category = null,
                description = null,
                language = null,
                mediaType = null,
            )
        },
    )

    private fun episodeFeed(vararg episodeUuids: String) = listFeed(
        episodes = episodeUuids.map { uuid ->
            DiscoverEpisode(
                uuid = uuid,
                title = "Episode $uuid",
                url = null,
                published = null,
                duration = null,
                fileType = null,
                size = null,
                podcast_uuid = "podcast-$uuid",
                podcast_title = "Podcast $uuid",
                type = null,
                season = null,
                number = null,
            )
        },
    )

    private fun listFeed(
        title: String? = null,
        subtitle: String? = null,
        podcasts: List<DiscoverPodcast>? = null,
        episodes: List<DiscoverEpisode>? = null,
    ) = ListFeed(
        title = title,
        subtitle = subtitle,
        description = null,
        shortDescription = null,
        date = null,
        podcasts = podcasts,
        episodes = episodes,
        podroll = null,
        collectionImageUrl = null,
        collectionRectangleImageUrl = null,
        featureImage = null,
        headerImageUrl = null,
        tintColors = null,
        collageImages = null,
        webLinkUrl = null,
        webLinkTitle = null,
        promotion = null,
    )
}
