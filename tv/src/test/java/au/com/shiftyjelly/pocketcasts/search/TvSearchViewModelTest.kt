package au.com.shiftyjelly.pocketcasts.search

import android.content.Context
import android.content.res.Resources
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverFeedLoader
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverRow
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
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

    @Test
    fun `exposes the loaded browse categories`() = runTest {
        whenever(listRepository.getCategoriesList(any())).thenReturn(
            listOf(category(1, "Comedy"), category(2, "True Crime")),
        )

        val viewModel = createViewModel()

        assertEquals(listOf("Comedy", "True Crime"), viewModel.categories.value.map { it.name })
    }

    @Test
    fun `categories are empty when loading fails`() = runTest {
        whenever(listRepository.getCategoriesList(any())).thenThrow(RuntimeException("Network error"))

        val viewModel = createViewModel()

        assertTrue(viewModel.categories.value.isEmpty())
    }

    @Test
    fun `exposes the loaded discover rows`() = runTest {
        whenever(listRepository.getDiscoverFeed()).thenReturn(
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
    fun `discover rows are empty when loading fails`() = runTest {
        whenever(listRepository.getDiscoverFeed()).thenThrow(RuntimeException("Network error"))

        val viewModel = createViewModel()

        assertTrue(viewModel.discoverRows.value.isEmpty())
    }

    private fun createViewModel() = TvSearchViewModel(
        listRepository = listRepository,
        discoverFeedLoader = TvDiscoverFeedLoader(
            listRepository = listRepository,
            settings = settings,
            context = context,
        ),
        syncManager = syncManager,
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
    ) = DiscoverRow(
        id = id,
        type = ListType.PodcastList,
        displayStyle = DisplayStyle.SmallList(),
        expandedStyle = ExpandedStyle.PlainList(),
        expandedTopItemLabel = null,
        title = title,
        source = source,
        listUuid = id,
        categoryId = null,
        regions = listOf("us"),
        curated = false,
        sponsored = false,
        authenticated = false,
        mostPopularCategoriesId = null,
        sponsoredCategoryIds = null,
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
