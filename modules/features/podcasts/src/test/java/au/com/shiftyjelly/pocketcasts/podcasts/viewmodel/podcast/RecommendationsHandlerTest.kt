package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class RecommendationsHandlerTest {

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private lateinit var listWebService: ListWebService
    private lateinit var podcastManager: PodcastManager
    private lateinit var recommendations: RecommendationsHandler
    private lateinit var testPodcast: Podcast
    private lateinit var testListFeed: ListFeed
    private lateinit var testDiscoverPodcasts: List<DiscoverPodcast>

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

        testPodcast = Podcast(uuid = UUID.randomUUID().toString())
        testDiscoverPodcasts = listOf(
            DiscoverPodcast(uuid = UUID.randomUUID().toString(), title = "Test Podcast 1", url = null, author = null, category = null, description = null, language = null, mediaType = null),
            DiscoverPodcast(uuid = UUID.randomUUID().toString(), title = "Test Podcast 2", url = null, author = null, category = null, description = null, language = null, mediaType = null),
        )
        testListFeed = ListFeed(title = "Test List Feed", podcasts = testDiscoverPodcasts, subtitle = null, description = null, date = null, episodes = null, collectionImageUrl = null, featureImage = null, headerImageUrl = null, tintColors = null, collageImages = null, webLinkUrl = null, webLinkTitle = null, promotion = null, podroll = null)
    }

    @Test
    fun testFeatureFlagDisabledNoPodcastsFetched() {
        // feature flag is disabled
        FeatureFlag.setEnabled(feature = Feature.RECOMMENDATIONS, enabled = false)

        recommendations.setEnabled(true)

        val list = recommendations.getRecommendationsFlowable(testPodcast)
            .test()
            .awaitCount(1)
            .assertNoErrors()
            .values()
            .first()

        // no podcasts should be found
        assertTrue(list is RecommendationsResult.Empty)
    }

    @Test
    fun testEnabledAndFeatureFlagOnPodcastsFetched() = runTest {
        FeatureFlag.setEnabled(feature = Feature.RECOMMENDATIONS, enabled = true)
        recommendations.setEnabled(true)

        // expected URL for the list recommendation
        val listUrl = "${Settings.SERVER_API_URL}/recommendations/podcast/${testPodcast.uuid}?country=us"
        whenever(listWebService.getListFeed(listUrl)).thenReturn(testListFeed)

        // mark the first podcast as subscribed
        val subscribedUuid = testDiscoverPodcasts.first().uuid
        whenever(podcastManager.getSubscribedPodcastUuidsRxSingle()).thenReturn(Single.just(listOf(subscribedUuid)))
        whenever(podcastManager.podcastSubscriptionsRxFlowable()).thenReturn(Flowable.just(emptyList()))

        // call the method to test
        val list = recommendations.getRecommendationsFlowable(testPodcast)
            .test()
            .awaitCount(1)
            .assertNoErrors()
            .values()
            .first()

        // check that the podcasts are fetched correctly
        assertTrue(list is RecommendationsResult.Success)

        val podcasts = (list as RecommendationsResult.Success).listFeed.podcasts
        assertEquals(testListFeed.podcasts?.size, podcasts?.size)

        // check that the subscribed status is set correctly
        val subscribedPodcast = podcasts?.find { it.uuid == subscribedUuid }
        val unsubscribedPodcast = podcasts?.find { it.uuid != subscribedUuid }

        assertTrue(subscribedPodcast?.isSubscribed == true)
        assertTrue(unsubscribedPodcast?.isSubscribed == false)
    }
}
