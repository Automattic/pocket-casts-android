package au.com.shiftyjelly.pocketcasts.search

import android.content.Context
import android.content.res.Resources
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverFeedLoader
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverRow
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.ImprovedSearchResultItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchHistoryEntry
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.search.ImprovedSearchManager
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.model.Discover
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRegion
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRow
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ExpandedStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import java.util.Date
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TvSearchViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val listRepository = mock<ListRepository>()
    private val syncManager = mock<SyncManager> {
        on { isLoggedIn() } doReturn false
    }
    private val resources = mock<Resources>()
    private val context = mock<Context> {
        whenever(it.resources).thenReturn(resources)
    }
    private val discoverCountryCode = mock<UserSetting<String>> {
        whenever(it.value).thenReturn("us")
    }
    private val settings = mock<Settings> {
        whenever(it.discoverCountryCode).thenReturn(discoverCountryCode)
    }
    private val improvedSearchManager = mock<ImprovedSearchManager>()
    private val podcastManager = mock<PodcastManager> {
        whenever(it.findSubscribedFlow(any())).thenReturn(flowOf(emptyList()))
    }
    private val episodeManager = mock<EpisodeManager>()
    private val playbackManager = mock<PlaybackManager>()
    private val searchHistoryManager = mock<SearchHistoryManager>()

    init {
        whenever { improvedSearchManager.autoCompleteSearch(any()) }.thenReturn(emptyList())
        whenever { searchHistoryManager.findAll(any()) }.thenReturn(emptyList())
    }

    @Test
    fun `exposes the browse categories from the search feed row`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(
            discover(categoriesRow(source = "https://categories.json")),
        )
        whenever(listRepository.getCategoriesList(eq("https://categories.json")))
            .thenReturn(listOf(category(1, "Comedy"), category(2, "True Crime")))

        val viewModel = createViewModel()

        assertEquals(listOf("Comedy", "True Crime"), viewModel.categories.value.map { it.name })
    }

    @Test
    fun `categories are empty when the feed has no categories row`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(
            discover(row(id = "trending", title = "Trending", source = "https://lists/trending.json")),
        )

        val viewModel = createViewModel()

        assertTrue(viewModel.categories.value.isEmpty())
    }

    @Test
    fun `categories are empty when the categories request fails`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(
            discover(categoriesRow(source = "https://categories.json")),
        )
        whenever(listRepository.getCategoriesList(any())).thenThrow(RuntimeException("Network error"))

        val viewModel = createViewModel()

        assertTrue(viewModel.categories.value.isEmpty())
    }

    @Test
    fun `exposes the loaded discover rows`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(
            discover(row(id = "trending", title = "Trending", source = "https://lists/trending.json")),
        )
        whenever(listRepository.getListFeed(eq("https://lists/trending.json"), any()))
            .thenReturn(podcastFeed("podcast-trending"))

        val viewModel = createViewModel()

        val rows = viewModel.discoverRows.value
        assertEquals(listOf("trending"), rows.map { it.id })
        assertTrue(rows.single() is TvDiscoverRow.Podcasts)
    }

    @Test
    fun `banner rows are excluded from the search discover feed`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(
            discover(
                bannerRow(id = "create_account"),
                row(id = "trending", title = "Trending", source = "https://lists/trending.json"),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://lists/trending.json"), any()))
            .thenReturn(podcastFeed("podcast-trending"))

        val viewModel = createViewModel()

        assertEquals(listOf("trending"), viewModel.discoverRows.value.map { it.id })
    }

    @Test
    fun `authenticated rows are dropped when logged out`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(
            discover(
                row(id = "public", title = "Public", source = "https://lists/public.json"),
                row(id = "members", title = "Members", source = "https://lists/members.json", authenticated = true),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://lists/public.json"), any()))
            .thenReturn(podcastFeed("podcast-public"))
        whenever(listRepository.getListFeed(eq("https://lists/members.json"), any()))
            .thenReturn(podcastFeed("podcast-members"))

        val viewModel = createViewModel()

        assertEquals(listOf("public"), viewModel.discoverRows.value.map { it.id })
        verify(listRepository, never()).getListFeed(eq("https://lists/members.json"), any())
    }

    @Test
    fun `authenticated rows are kept when logged in`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(true)
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(
            discover(
                row(id = "public", title = "Public", source = "https://lists/public.json"),
                row(id = "members", title = "Members", source = "https://lists/members.json", authenticated = true),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://lists/public.json"), any()))
            .thenReturn(podcastFeed("podcast-public"))
        whenever(listRepository.getListFeed(eq("https://lists/members.json"), any()))
            .thenReturn(podcastFeed("podcast-members"))

        val viewModel = createViewModel()

        assertEquals(listOf("public", "members"), viewModel.discoverRows.value.map { it.id })
    }

    @Test
    fun `single podcast display style maps to a single podcast row`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(
            discover(
                row(
                    id = "spotlight",
                    title = "Spotlight",
                    source = "https://lists/spotlight.json",
                    displayStyle = DisplayStyle.SinglePodcast(),
                ),
            ),
        )
        whenever(listRepository.getListFeed(eq("https://lists/spotlight.json"), any()))
            .thenReturn(podcastFeed("podcast-spotlight"))

        val viewModel = createViewModel()

        assertTrue(viewModel.discoverRows.value.single() is TvDiscoverRow.SinglePodcast)
    }

    @Test
    fun `discover rows are empty when loading fails`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenThrow(RuntimeException("Network error"))

        val viewModel = createViewModel()

        assertTrue(viewModel.discoverRows.value.isEmpty())
    }

    @Test
    fun `categoryPodcasts maps the loaded category feed`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever(listRepository.getHomeDiscoverFeed(isLoggedIn = false)).thenReturn(discover())
        whenever(listRepository.getListFeed(eq("https://category/us.json"), any()))
            .thenReturn(podcastFeed("podcast-1", "podcast-2"))

        val podcasts = createViewModel().categoryPodcasts(categoryId = 7, source = "https://category/us.json")

        assertEquals(listOf("podcast-1", "podcast-2"), podcasts.map { it.uuid })
    }

    @Test
    fun `categories still load when building the discover rows fails`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(
            Discover(
                layout = listOf(categoriesRow(source = "https://categories.json")),
                regions = emptyMap(),
                regionCodeToken = "[regionCode]",
                regionNameToken = "[regionName]",
                defaultRegionCode = "us",
            ),
        )
        whenever(listRepository.getCategoriesList(eq("https://categories.json")))
            .thenReturn(listOf(category(1, "Comedy")))

        val viewModel = createViewModel()

        assertEquals(listOf("Comedy"), viewModel.categories.value.map { it.name })
        assertTrue(viewModel.discoverRows.value.isEmpty())
    }

    @Test
    fun `a blank query stays idle`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        val viewModel = createViewModel()

        viewModel.onQueryChange("   ")
        advanceUntilIdle()

        assertEquals(TvSearchState.Idle, viewModel.searchState.value)
    }

    @Test
    fun `combined results are split into podcasts and episodes`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(
            listOf(podcastItem("podcast-1"), episodeItem("episode-1")),
        )

        val viewModel = createViewModel()
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        val state = viewModel.searchState.value as TvSearchState.Results
        assertEquals(listOf("podcast-1"), state.podcasts.map { it.uuid })
        assertEquals(listOf("episode-1"), state.episodes.map { it.uuid })
    }

    @Test
    fun `episodes are de-duplicated by uuid`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(
            listOf(episodeItem("episode-1"), episodeItem("episode-1"), episodeItem("episode-2")),
        )

        val viewModel = createViewModel()
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        val state = viewModel.searchState.value as TvSearchState.Results
        assertEquals(listOf("episode-1", "episode-2"), state.episodes.map { it.uuid })
    }

    @Test
    fun `keystrokes within the debounce window only search the last term`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(emptyList())

        val viewModel = createViewModel()
        viewModel.onQueryChange("su")
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        verifyBlocking(improvedSearchManager) { combinedSearch(eq("sugar")) }
        verifyBlocking(improvedSearchManager, never()) { combinedSearch(eq("su")) }
    }

    @Test
    fun `empty combined results report no results`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(emptyList())

        val viewModel = createViewModel()
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        assertEquals(TvSearchState.NoResults, viewModel.searchState.value)
    }

    @Test
    fun `server results lead and subscribed podcasts fill the remaining gaps`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever(podcastManager.findSubscribedFlow(any())).thenReturn(flowOf(listOf(subscribedPodcast("podcast-1"))))
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(
            listOf(podcastItem("podcast-2")),
        )

        val viewModel = createViewModel()
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        val state = viewModel.searchState.value as TvSearchState.Results
        assertEquals(listOf("podcast-2", "podcast-1"), state.podcasts.map { it.uuid })
    }

    @Test
    fun `predictive term results are exposed as suggestions`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.autoCompleteSearch(any()) }.thenReturn(
            listOf(SearchAutoCompleteItem.Term("sugar rush"), SearchAutoCompleteItem.Term("sugar high")),
        )
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(emptyList())

        val viewModel = createViewModel()
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        assertEquals(listOf("sugar rush", "sugar high"), viewModel.suggestions.value)
    }

    @Test
    fun `searching a term saves it to history and exposes recent searches`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(emptyList())
        whenever { searchHistoryManager.findAll(any()) }.thenReturn(
            listOf(SearchHistoryEntry.SearchTerm(term = "sugar")),
        )

        val viewModel = createViewModel()
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        verifyBlocking(searchHistoryManager) { add(any()) }
        verifyBlocking(searchHistoryManager) { truncateHistory(20) }
        assertEquals(listOf("sugar"), viewModel.history.value)
    }

    @Test
    fun `clearing the query clears the suggestions`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.autoCompleteSearch(any()) }.thenReturn(
            listOf(SearchAutoCompleteItem.Term("sugar")),
        )
        whenever { improvedSearchManager.combinedSearch(any()) }.thenReturn(emptyList())
        val viewModel = createViewModel()
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()
        assertTrue(viewModel.suggestions.value.isNotEmpty())

        viewModel.onQueryChange("")

        assertTrue(viewModel.suggestions.value.isEmpty())
    }

    @Test
    fun `a search failure surfaces the error state`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        whenever { improvedSearchManager.combinedSearch(any()) }.thenThrow(RuntimeException("Network error"))

        val viewModel = createViewModel()
        viewModel.onQueryChange("sugar")
        advanceUntilIdle()

        assertEquals(TvSearchState.Error, viewModel.searchState.value)
    }

    @Test
    fun `onFilterSelected updates the filter`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        val viewModel = createViewModel()

        viewModel.onFilterSelected(TvSearchFilter.Episodes)

        assertEquals(TvSearchFilter.Episodes, viewModel.filter.value)
    }

    @Test
    fun `clearing the query preserves the selected filter`() = runTest {
        whenever(listRepository.getSearchDiscoverFeed()).thenReturn(discover())
        val viewModel = createViewModel()

        viewModel.onFilterSelected(TvSearchFilter.Podcasts)
        viewModel.onQueryChange("")
        advanceUntilIdle()

        assertEquals(TvSearchFilter.Podcasts, viewModel.filter.value)
    }

    private fun podcastItem(uuid: String) = ImprovedSearchResultItem.PodcastItem(
        uuid = uuid,
        title = "Podcast $uuid",
        author = "Author",
        isFollowed = false,
    )

    private fun episodeItem(uuid: String) = ImprovedSearchResultItem.EpisodeItem(
        uuid = uuid,
        title = "Episode $uuid",
        podcastUuid = "podcast-$uuid",
        podcastTitle = "Podcast",
        publishedDate = Date(0),
        duration = 120.seconds,
    )

    private fun subscribedPodcast(uuid: String) = Podcast(uuid = uuid, title = "Podcast $uuid", author = "Author")

    private fun createViewModel() = TvSearchViewModel(
        discoverFeedLoader = TvDiscoverFeedLoader(
            listRepository = listRepository,
            settings = settings,
            context = context,
        ),
        syncManager = syncManager,
        improvedSearchManager = improvedSearchManager,
        podcastManager = podcastManager,
        episodeManager = episodeManager,
        playbackManager = playbackManager,
        searchHistoryManager = searchHistoryManager,
    )

    private fun category(id: Int, name: String) = DiscoverCategory(id = id, name = name, icon = "", source = "")

    private fun discover(vararg rows: DiscoverRow) = Discover(
        layout = rows.toList(),
        regions = mapOf("us" to DiscoverRegion(name = "United States", flag = "flag", code = "us")),
        regionCodeToken = "[regionCode]",
        regionNameToken = "[regionName]",
        defaultRegionCode = "us",
    )

    private fun row(
        id: String,
        title: String,
        source: String,
        type: ListType = ListType.PodcastList,
        displayStyle: DisplayStyle = DisplayStyle.SmallList(),
        authenticated: Boolean = false,
    ) = DiscoverRow(
        id = id,
        type = type,
        displayStyle = displayStyle,
        expandedStyle = ExpandedStyle.PlainList(),
        expandedTopItemLabel = null,
        title = title,
        source = source,
        listUuid = id,
        categoryId = null,
        regions = listOf("us"),
        curated = false,
        sponsored = false,
        authenticated = authenticated,
        mostPopularCategoriesId = null,
        sponsoredCategoryIds = null,
    )

    private fun categoriesRow(source: String) = row(
        id = "categories",
        title = "Browse By Category",
        source = source,
        type = ListType.Categories,
    )

    private fun bannerRow(id: String) = row(
        id = id,
        title = "",
        source = "",
        type = ListType.Unknown("banner"),
        displayStyle = DisplayStyle.Unknown("inline_banner"),
    )

    private fun podcastFeed(vararg podcastUuids: String) = ListFeed(
        title = null,
        subtitle = null,
        description = null,
        shortDescription = null,
        date = null,
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
        episodes = null,
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
