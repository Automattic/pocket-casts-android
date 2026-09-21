package au.com.shiftyjelly.pocketcasts.repositories.lists

import au.com.shiftyjelly.pocketcasts.servers.model.Discover
import au.com.shiftyjelly.pocketcasts.servers.server.ListWebService
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ListRepositoryTest {

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val webService = mock<ListWebService> {
        on { getDiscoverFeed(any(), any()) } doReturn EMPTY_DISCOVER
        on { getSearchDiscoverFeed(any(), any()) } doReturn EMPTY_DISCOVER
        on { getLoggedInDiscoverFeed(any(), any()) } doReturn EMPTY_DISCOVER
        on { getLoggedOutDiscoverFeed(any(), any()) } doReturn EMPTY_DISCOVER
    }

    private val repository = ListRepository(
        listWebService = webService,
        syncManager = mock(),
        platform = PLATFORM,
    )

    @Test
    fun `discover feed uses v3 when networks are disabled`() = runTest {
        FeatureFlag.setEnabled(Feature.NETWORK_DISCOVERY, false)

        repository.getDiscoverFeed()

        verify(webService).getDiscoverFeed(PLATFORM, VERSION_WITHOUT_NETWORKS)
    }

    @Test
    fun `discover feed uses v4 when networks are enabled`() = runTest {
        FeatureFlag.setEnabled(Feature.NETWORK_DISCOVERY, true)

        repository.getDiscoverFeed()

        verify(webService).getDiscoverFeed(PLATFORM, VERSION_WITH_NETWORKS)
    }

    @Test
    fun `search discover feed uses v3 when networks are disabled`() = runTest {
        FeatureFlag.setEnabled(Feature.NETWORK_DISCOVERY, false)

        repository.getSearchDiscoverFeed()

        verify(webService).getSearchDiscoverFeed(PLATFORM, VERSION_WITHOUT_NETWORKS)
    }

    @Test
    fun `search discover feed uses v4 when networks are enabled`() = runTest {
        FeatureFlag.setEnabled(Feature.NETWORK_DISCOVERY, true)

        repository.getSearchDiscoverFeed()

        verify(webService).getSearchDiscoverFeed(PLATFORM, VERSION_WITH_NETWORKS)
    }

    @Test
    fun `logged in home discover feed uses v3 when networks are disabled`() = runTest {
        FeatureFlag.setEnabled(Feature.NETWORK_DISCOVERY, false)

        repository.getHomeDiscoverFeed(isLoggedIn = true)

        verify(webService).getLoggedInDiscoverFeed(PLATFORM, VERSION_WITHOUT_NETWORKS)
    }

    @Test
    fun `logged in home discover feed uses v4 when networks are enabled`() = runTest {
        FeatureFlag.setEnabled(Feature.NETWORK_DISCOVERY, true)

        repository.getHomeDiscoverFeed(isLoggedIn = true)

        verify(webService).getLoggedInDiscoverFeed(PLATFORM, VERSION_WITH_NETWORKS)
    }

    @Test
    fun `logged out home discover feed uses v3 when networks are disabled`() = runTest {
        FeatureFlag.setEnabled(Feature.NETWORK_DISCOVERY, false)

        repository.getHomeDiscoverFeed(isLoggedIn = false)

        verify(webService).getLoggedOutDiscoverFeed(PLATFORM, VERSION_WITHOUT_NETWORKS)
    }

    @Test
    fun `logged out home discover feed uses v4 when networks are enabled`() = runTest {
        FeatureFlag.setEnabled(Feature.NETWORK_DISCOVERY, true)

        repository.getHomeDiscoverFeed(isLoggedIn = false)

        verify(webService).getLoggedOutDiscoverFeed(PLATFORM, VERSION_WITH_NETWORKS)
    }

    companion object {
        private const val PLATFORM = ListRepository.PLATFORM_ANDROID
        private const val VERSION_WITHOUT_NETWORKS = 3
        private const val VERSION_WITH_NETWORKS = 4

        private val EMPTY_DISCOVER = Discover(
            layout = emptyList(),
            regions = emptyMap(),
            regionCodeToken = "[regionCode]",
            regionNameToken = "[regionName]",
            defaultRegionCode = "us",
        )
    }
}
