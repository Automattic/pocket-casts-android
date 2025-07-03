package au.com.shiftyjelly.pocketcasts.discover.util

import android.content.res.Resources
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.servers.model.Discover
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRegion
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRow
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ExpandedStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.servers.model.SponsoredPodcast
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DiscoverDeepLinkManagerTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var mockRepository: ListRepository

    @Mock
    private lateinit var mockSettings: Settings

    @Mock
    private lateinit var mockResources: Resources

    private lateinit var manager: DiscoverDeepLinkManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        manager = DiscoverDeepLinkManager(mockRepository, mockSettings)
    }

    @Test
    fun `loadStaffPicks should return transformed staff picks row`() = runTest {
        val staffPicksRow = createDiscoverRow(
            listUuid = "staff-picks",
            title = "Popular in {region_name}",
            source = "source_{region_code}",
            expandedTopItemLabel = "Top in {region_name}",
        )
        val discover = createTestDiscover(layout = listOf(staffPicksRow))

        whenever(mockRepository.getDiscoverFeed()).thenReturn(discover)

        val discoverCountryCodeMock: UserSetting<String> = mock()
        whenever(discoverCountryCodeMock.flow).thenReturn(MutableStateFlow("US"))
        whenever(mockSettings.discoverCountryCode).thenReturn(discoverCountryCodeMock)

        whenever(mockResources.getString(any())).thenAnswer { it.arguments[0].toString() }

        val result = manager.getDiscoverList("staff-picks", mockResources)

        assert(result != null)
        assert(result?.listUuid == "staff-picks")
    }

    @Test
    fun `loadStaffPicks should return null when region not in row regions`() = runTest {
        val row = createDiscoverRow(
            listUuid = "staff-picks",
            regions = listOf("JP"),
        )
        val discover = createTestDiscover(layout = listOf(row))

        whenever(mockRepository.getDiscoverFeed()).thenReturn(discover)

        val discoverCountryCodeMock: UserSetting<String> = mock()
        whenever(discoverCountryCodeMock.flow).thenReturn(MutableStateFlow("US"))
        whenever(mockSettings.discoverCountryCode).thenReturn(discoverCountryCodeMock)

        val result = manager.getDiscoverList("staff-picks", mockResources)

        assert(result == null)
    }

    private fun createTestDiscover(
        layout: List<DiscoverRow> = listOf(createDiscoverRow(listUuid = "staff-picks")),
        regions: Map<String, DiscoverRegion> = mapOf(
            "US" to DiscoverRegion("United States", "US-flag", "US"),
            "UK" to DiscoverRegion("United Kingdom", "UK-flag", "UK"),
        ),
        regionCodeToken: String = "{region_code}",
        regionNameToken: String = "{region_name}",
        defaultRegionCode: String = "US",
    ): Discover {
        return Discover(
            layout = layout,
            regions = regions,
            regionCodeToken = regionCodeToken,
            regionNameToken = regionNameToken,
            defaultRegionCode = defaultRegionCode,
        )
    }

    private fun createDiscoverRow(
        id: String? = null,
        type: ListType = ListType.PodcastList,
        displayStyle: DisplayStyle = DisplayStyle.SmallList(),
        expandedStyle: ExpandedStyle = ExpandedStyle.GridList(),
        expandedTopItemLabel: String? = null,
        title: String = "Title",
        source: String = "Source",
        listUuid: String? = "staff-picks",
        categoryId: Int? = null,
        regions: List<String> = listOf("US", "UK"),
        sponsored: Boolean = false,
        curated: Boolean = false,
        sponsoredPodcasts: List<SponsoredPodcast> = emptyList(),
        mostPopularCategoriesId: List<Int>? = null,
        sponsoredCategoryIds: List<Int>? = null,
    ): DiscoverRow {
        return DiscoverRow(
            id = id,
            type = type,
            displayStyle = displayStyle,
            expandedStyle = expandedStyle,
            expandedTopItemLabel = expandedTopItemLabel,
            title = title,
            source = source,
            listUuid = listUuid,
            categoryId = categoryId,
            regions = regions,
            sponsored = sponsored,
            curated = curated,
            sponsoredPodcasts = sponsoredPodcasts,
            mostPopularCategoriesId = mostPopularCategoriesId,
            sponsoredCategoryIds = sponsoredCategoryIds,
        )
    }
}
