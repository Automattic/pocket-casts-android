package au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations

import android.app.Application
import android.content.res.Resources
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingRecommendationsStartPageViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.categories.CategoriesManager
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.model.Discover
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRegion
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRow
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ExpandedStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import io.reactivex.Flowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingRecommendationsStartPageViewModelTest {
    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val podcastManager = mock<PodcastManager>()
    private val playbackManager = mock<PlaybackManager>()
    private val analyticsTracker = mock<AnalyticsTracker>()
    private val repository = mock<ListRepository>()
    private val settings = mock<Settings>()
    private val categoriesManager = mock<CategoriesManager>()
    private val application = mock<Application>()

    @Before
    fun setup() = runTest {
        whenever(podcastManager.subscribedRxFlowable()).thenReturn(Flowable.just(emptyList()))
        whenever(repository.getDiscoverFeed()).thenReturn(mockDiscover)
        whenever(repository.getCategoriesList(any())).thenReturn(mockCategories)
        whenever(repository.getListFeed(any(), any())).thenReturn(mockListFeed)
        val discoverCountryCodeMock = mock<UserSetting<String>>()
        whenever(discoverCountryCodeMock.value).thenReturn("US")
        whenever(settings.discoverCountryCode).thenReturn(discoverCountryCodeMock)
        val mockResources = mock<Resources>()
        whenever(mockResources.getString(any())).thenReturn("")
        whenever(application.resources).thenReturn(mockResources)
    }

    @Test
    fun `should prioritize interest categories when FF is on and interests are set`() = runTest {
        FeatureFlag.setEnabled(Feature.NEW_ONBOARDING_RECOMMENDATIONS, true)
        whenever(categoriesManager.interestCategories).thenReturn(MutableStateFlow(mockCategories.takeLast(3).toSet()).asStateFlow())

        val viewModel = createViewModel()
        val state = viewModel.state.value
        viewModel.state.test {
            val item = awaitItem()
            assert(item.sections.take(3).map { it.title }.all { it in mockCategories.takeLast(3).map { it.title } })
        }
    }

    @Test
    fun `should return normal recommendations when FF is on but interests are empty`() = runTest {
        FeatureFlag.setEnabled(Feature.NEW_ONBOARDING_RECOMMENDATIONS, true)
        whenever(categoriesManager.interestCategories).thenReturn(MutableStateFlow(emptySet()))

        val viewModel = createViewModel()
        viewModel.state.test {
            val item = awaitItem()
            assert(item.sections.take(3).map { it.title }.all { it in mockCategories.take(3).map { it.title } })
        }
    }

    @Test
    fun `should return normal recommendations when FF is off`() = runTest {
        FeatureFlag.setEnabled(Feature.NEW_ONBOARDING_RECOMMENDATIONS, false)
        whenever(categoriesManager.interestCategories).thenReturn(MutableStateFlow(emptySet()))

        val viewModel = createViewModel()
        viewModel.state.test {
            val item = awaitItem()
            assert(item.sections.take(2).map { it.title }.all { it in mockCategories.take(3).map { it.title } })
        }
    }

    private fun createViewModel() = OnboardingRecommendationsStartPageViewModel(
        podcastManager = podcastManager,
        playbackManager = playbackManager,
        analyticsTracker = analyticsTracker,
        repository = repository,
        settings = settings,
        app = application,
        categoriesManager = categoriesManager,
    )

    private companion object {
        val mockDiscover = Discover(
            layout = listOf(
                DiscoverRow(
                    id = "id",
                    type = ListType.Categories,
                    displayStyle = DisplayStyle.Category(),
                    expandedStyle = ExpandedStyle.GridList(),
                    expandedTopItemLabel = null,
                    title = "title",
                    source = "source",
                    listUuid = "listUuid",
                    categoryId = 1,
                    regions = listOf("US"),
                    sponsored = false,
                    curated = false,
                    mostPopularCategoriesId = null,
                    sponsoredCategoryIds = null,
                ),
            ),
            regions = mapOf(
                "US" to DiscoverRegion(
                    name = "name",
                    flag = "",
                    code = "US",
                ),
            ),
            regionCodeToken = "{nope}",
            regionNameToken = "{nope}",
            defaultRegionCode = "US",
        )

        val mockCategories = List(10) {
            DiscoverCategory(
                id = it,
                name = "Category $it",
                icon = "",
                source = "",
            )
        }

        val mockListFeed = ListFeed(
            title = "title",
            subtitle = "subtitle",
            description = "description",
            shortDescription = "shortDescription",
            date = "date",
            podcasts = List(4) {
                DiscoverPodcast(
                    uuid = "uuid $it",
                    title = "podcast $it",
                    url = "",
                    author = "",
                    category = "",
                    description = "description",
                    language = "language",
                    mediaType = "mediaType",
                )
            },
            episodes = emptyList(),
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
}
