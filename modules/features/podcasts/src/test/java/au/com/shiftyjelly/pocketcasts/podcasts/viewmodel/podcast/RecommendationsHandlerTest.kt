package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.podcast

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import au.com.shiftyjelly.pocketcasts.servers.server.ListWebService
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import io.reactivex.Flowable
import io.reactivex.Single
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class RecommendationsHandlerTest {

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private lateinit var listWebService: ListWebService
    private lateinit var podcastManager: PodcastManager
    private lateinit var recommendations: RecommendationsHandler
    private lateinit var testPodcastUuid: String
    private lateinit var testListFeed: ListFeed
    private lateinit var testDiscoverPodcasts: List<DiscoverPodcast>
    private lateinit var testPodrollPodcasts: List<DiscoverPodcast>

    @Before
    fun setup() {
        listWebService = mock()
        val listRepository = ListRepository(listWebService = listWebService, syncManager = null, platform = "android")
        podcastManager = mock()

        val discoverCountryCode = mock<UserSetting<String>>()
        whenever(discoverCountryCode.value).thenReturn("us")
        val settings = mock<Settings>()
        whenever(settings.discoverCountryCode).thenReturn(discoverCountryCode)

        recommendations = RecommendationsHandler(listRepository, podcastManager, settings)

        testPodcastUuid = UUID.randomUUID().toString()
        testDiscoverPodcasts = listOf(
            DiscoverPodcast(uuid = UUID.randomUUID().toString(), title = "Test Podcast 1", url = null, author = null, category = null, description = null, language = null, mediaType = null),
            DiscoverPodcast(uuid = UUID.randomUUID().toString(), title = "Test Podcast 2", url = null, author = null, category = null, description = null, language = null, mediaType = null),
        )
        testPodrollPodcasts = listOf(
            DiscoverPodcast(uuid = UUID.randomUUID().toString(), title = "Test Podcast 3", url = null, author = null, category = null, description = null, language = null, mediaType = null),
            DiscoverPodcast(uuid = UUID.randomUUID().toString(), title = "Test Podcast 4", url = null, author = null, category = null, description = null, language = null, mediaType = null),
        )
        testListFeed = ListFeed(title = "Test List Feed", podcasts = testDiscoverPodcasts, podroll = testPodrollPodcasts, subtitle = null, description = null, shortDescription = null, date = null, episodes = null, collectionImageUrl = null, collectionRectangleImageUrl = null, featureImage = null, headerImageUrl = null, tintColors = null, collageImages = null, webLinkUrl = null, webLinkTitle = null, promotion = null)
    }

    @Test
    fun `do not load recommendations when feature flag is disabled`() = runTest {
        // feature flag is disabled
        FeatureFlag.setEnabled(feature = Feature.RECOMMENDATIONS, enabled = false)
        recommendations.setEnabled(true)

        whenever(listWebService.getListFeed(any())).thenReturn(testListFeed)

        val values = recommendations.getRecommendationsFlowable(testPodcastUuid)
            .test()
            .awaitCount(2)
            .assertNoErrors()
            .values()

        // no podcasts should be found
        assertTrue(values[0] is RecommendationsResult.Loading)
        assertTrue(values[1] is RecommendationsResult.Empty)
    }

    @Test
    fun `do not load recommendations when recommendations are disabled`() = runTest {
        // feature flag is disabled
        FeatureFlag.setEnabled(feature = Feature.RECOMMENDATIONS, enabled = true)
        recommendations.setEnabled(false)

        whenever(listWebService.getListFeed(any())).thenReturn(testListFeed)

        val values = recommendations.getRecommendationsFlowable(testPodcastUuid)
            .test()
            .awaitCount(2)
            .assertNoErrors()
            .values()

        // no podcasts should be found
        assertTrue(values[0] is RecommendationsResult.Loading)
        assertTrue(values[1] is RecommendationsResult.Empty)
    }

    @Test
    fun `load recommendations when podcasts are available`() = runTest {
        FeatureFlag.setEnabled(feature = Feature.RECOMMENDATIONS, enabled = true)
        recommendations.setEnabled(true)

        // expected URL for the list recommendation
        val listUrl = "${Settings.SERVER_API_URL}/recommendations/podcast/$testPodcastUuid?country=us"
        whenever(listWebService.getListFeed(listUrl)).thenReturn(testListFeed.copy(podroll = null))

        // mark the first podcast as subscribed
        val subscribedUuid = testDiscoverPodcasts.first().uuid
        whenever(podcastManager.getSubscribedPodcastUuidsRxSingle()).thenReturn(Single.just(listOf(subscribedUuid)))
        whenever(podcastManager.podcastSubscriptionsRxFlowable()).thenReturn(Flowable.empty())

        // call the method to test
        val values = recommendations.getRecommendationsFlowable(testPodcastUuid)
            .test()
            .awaitCount(2)
            .assertNoErrors()
            .values()

        // check that the podcasts are fetched correctly
        assertTrue(values[0] is RecommendationsResult.Loading)
        assertTrue(values[1] is RecommendationsResult.Success)

        val podcasts = (values[1] as RecommendationsResult.Success).listFeed.podcasts
        assertEquals(2, podcasts?.size)

        // check that the subscribed status is set correctly
        val subscribedPodcast = podcasts?.find { it.uuid == subscribedUuid }
        val unsubscribedPodcast = podcasts?.find { it.uuid != subscribedUuid }

        assertTrue(subscribedPodcast?.isSubscribed == true)
        assertTrue(unsubscribedPodcast?.isSubscribed == false)
    }

    @Test
    fun `load recommendations when podroll is available`() = runTest {
        FeatureFlag.setEnabled(feature = Feature.RECOMMENDATIONS, enabled = true)
        recommendations.setEnabled(true)

        // expected URL for the list recommendation
        val listUrl = "${Settings.SERVER_API_URL}/recommendations/podcast/$testPodcastUuid?country=us"
        whenever(listWebService.getListFeed(listUrl)).thenReturn(testListFeed.copy(podcasts = null))

        // mark the first podcast as subscribed
        val subscribedUuid = testPodrollPodcasts.first().uuid
        whenever(podcastManager.getSubscribedPodcastUuidsRxSingle()).thenReturn(Single.just(listOf(subscribedUuid)))
        whenever(podcastManager.podcastSubscriptionsRxFlowable()).thenReturn(Flowable.empty())

        // call the method to test
        val values = recommendations.getRecommendationsFlowable(testPodcastUuid)
            .test()
            .awaitCount(2)
            .assertNoErrors()
            .values()

        // check that the podcasts are fetched correctly
        assertTrue(values[0] is RecommendationsResult.Loading)
        assertTrue(values[1] is RecommendationsResult.Success)

        val podcasts = (values[1] as RecommendationsResult.Success).listFeed.podroll
        assertEquals(2, podcasts?.size)

        // check that the subscribed status is set correctly
        val subscribedPodcast = podcasts?.find { it.uuid == subscribedUuid }
        val unsubscribedPodcast = podcasts?.find { it.uuid != subscribedUuid }

        assertTrue(subscribedPodcast?.isSubscribed == true)
        assertTrue(unsubscribedPodcast?.isSubscribed == false)
    }

    @Test
    fun `do not load recommendations when neither podcasts or podroll are available`() = runTest {
        FeatureFlag.setEnabled(feature = Feature.RECOMMENDATIONS, enabled = true)
        recommendations.setEnabled(true)

        // expected URL for the list recommendation
        val listUrl = "${Settings.SERVER_API_URL}/recommendations/podcast/$testPodcastUuid?country=us"
        whenever(listWebService.getListFeed(listUrl)).thenReturn(testListFeed.copy(podroll = null, podcasts = null))

        whenever(podcastManager.getSubscribedPodcastUuidsRxSingle()).thenReturn(Single.just(emptyList()))
        whenever(podcastManager.podcastSubscriptionsRxFlowable()).thenReturn(Flowable.just(emptyList()))

        // call the method to test
        val values = recommendations.getRecommendationsFlowable(testPodcastUuid)
            .test()
            .awaitCount(2)
            .assertNoErrors()
            .values()

        // no podcasts should be found
        assertTrue(values[0] is RecommendationsResult.Loading)
        assertTrue(values[1] is RecommendationsResult.Empty)
    }
}
