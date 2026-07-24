package au.com.shiftyjelly.pocketcasts.home

import android.content.Context
import android.content.res.Resources
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.UpNextDao
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.model.Discover
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverEpisode
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRegion
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRow
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ExpandedStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalCoroutinesApi::class)
class TvHomeViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val listRepository = mock<ListRepository>()
    private val syncManager = mock<SyncManager>()
    private val podcastDao = mock<PodcastDao> {
        on { findAllIn(any()) }.thenReturn(emptyList())
    }
    private val upNextDao = mock<UpNextDao> {
        on { getUpNextBaseEpisodes(any()) }.thenReturn(emptyList())
    }
    private val playlistManager = mock<PlaylistManager> {
        whenever(it.smartEpisodesFlow(any(), any(), anyOrNull())).thenReturn(flowOf(emptyList()))
    }
    private val resources = mock<Resources>()
    private val context = mock<Context> {
        whenever(it.resources).thenReturn(resources)
        whenever(it.getString(LR.string.tv_home_keep_listening)).thenReturn("Keep Listening")
        whenever(it.getString(LR.string.up_next)).thenReturn("Up Next")
        whenever(it.getString(LR.string.filters_title_new_releases)).thenReturn("New Releases")
    }
    private val discoverCountryCode = mock<UserSetting<String>> {
        whenever(it.value).thenReturn("us")
    }
    private val settings = mock<Settings> {
        whenever(it.discoverCountryCode).thenReturn(discoverCountryCode)
    }

    @Test
    fun `all rows load in feed order`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getDiscoverFeed()).thenReturn(
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
            assertTrue(state.rows[0] is TvHomeRow.FeaturedPodcasts)
            assertTrue(state.rows[1] is TvHomeRow.FeaturedPodcasts)
            assertTrue(state.rows[2] is TvHomeRow.Episodes)
            assertTrue(state.rows[3] is TvHomeRow.Podcasts)
            assertTrue(state.rows[4] is TvHomeRow.Podcasts)
        }
    }

    @Test
    fun `authenticated rows are excluded when signed out`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getDiscoverFeed()).thenReturn(
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
    fun `authenticated rows load when signed in`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(true)
        whenever(listRepository.getDiscoverFeed()).thenReturn(
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
        whenever(listRepository.getDiscoverFeed()).thenReturn(
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
            val row = state.rows.single() as TvHomeRow.FeaturedPodcasts
            assertTrue(row.podcasts.single().isSponsored)
        }
    }

    @Test
    fun `rows that fail to load are dropped`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getDiscoverFeed()).thenReturn(
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
    fun `rows with duplicate ids are deduplicated`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getDiscoverFeed()).thenReturn(
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
        whenever(listRepository.getDiscoverFeed()).thenReturn(
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
        whenever(listRepository.getDiscoverFeed()).thenReturn(
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
    fun `feed failure shows error state and retry reloads`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getDiscoverFeed())
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
    fun `keep listening row shows first up next episode even when signed out`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getDiscoverFeed()).thenReturn(discover())
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
            val row = state.rows.single() as TvHomeRow.Episodes
            assertEquals("Keep Listening", row.title)
            val rowEpisode = row.episodes.single()
            assertEquals("episode-1", rowEpisode.episodeUuid)
            assertEquals("Podcast One", rowEpisode.podcastTitle)
        }
    }

    @Test
    fun `user episodes in up next are skipped`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getDiscoverFeed()).thenReturn(discover())
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
            val row = state.rows.single() as TvHomeRow.Episodes
            assertEquals(listOf("episode-1"), row.episodes.map { it.episodeUuid })
        }
    }

    @Test
    fun `signed in user sees keep listening, up next and new releases rows before discover rows`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(true)
        whenever(listRepository.getDiscoverFeed()).thenReturn(
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
        whenever(playlistManager.smartEpisodesFlow(any(), any(), anyOrNull())).thenReturn(
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
            val upNextRow = state.rows[1] as TvHomeRow.Episodes
            assertEquals(listOf("episode-2", "episode-3"), upNextRow.episodes.map { it.episodeUuid })
            val newReleasesRow = state.rows[2] as TvHomeRow.Episodes
            assertEquals(listOf("episode-new"), newReleasesRow.episodes.map { it.episodeUuid })
            assertEquals("Podcast Two", newReleasesRow.episodes.single().podcastTitle)
        }
    }

    @Test
    fun `up next and new releases rows are hidden when signed out`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(listRepository.getDiscoverFeed()).thenReturn(discover())
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

    private fun createViewModel() = TvHomeViewModel(
        listRepository = listRepository,
        playlistManager = playlistManager,
        podcastDao = podcastDao,
        upNextDao = upNextDao,
        settings = settings,
        syncManager = syncManager,
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
    ) = DiscoverRow(
        id = id,
        type = type,
        displayStyle = displayStyle,
        expandedStyle = ExpandedStyle.PlainList(),
        expandedTopItemLabel = null,
        title = title,
        source = source,
        listUuid = listUuid,
        categoryId = null,
        regions = regions,
        curated = curated,
        sponsored = sponsored,
        authenticated = authenticated,
        mostPopularCategoriesId = null,
        sponsoredCategoryIds = null,
    )

    private fun podcastFeed(vararg podcastUuids: String, title: String? = null) = listFeed(
        title = title,
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
        podcasts: List<DiscoverPodcast>? = null,
        episodes: List<DiscoverEpisode>? = null,
    ) = ListFeed(
        title = title,
        subtitle = null,
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
