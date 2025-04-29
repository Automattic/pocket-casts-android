package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SimilarPodcastHandlerTest {

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private lateinit var listWebService: ListWebService
    private lateinit var podcastManager: PodcastManager
    private lateinit var similarPodcasts: SimilarPodcastHandler
    private lateinit var testPodcast: Podcast
    private lateinit var testListFeed: ListFeed
    private lateinit var testDiscoverPodcasts: List<DiscoverPodcast>

    @Before
    fun setup() {
        listWebService = mock()
        val listRepository = ListRepository(listWebService = listWebService, syncManager = null, platform = "android")
        podcastManager = mock()
        similarPodcasts = SimilarPodcastHandler(listRepository, podcastManager)

        testPodcast = Podcast(uuid = UUID.randomUUID().toString())
        testDiscoverPodcasts = listOf(
            DiscoverPodcast(uuid = UUID.randomUUID().toString(), title = "Test Podcast 1", url = null, author = null, category = null, description = null, language = null, mediaType = null),
            DiscoverPodcast(uuid = UUID.randomUUID().toString(), title = "Test Podcast 2", url = null, author = null, category = null, description = null, language = null, mediaType = null),
        )
        testListFeed = ListFeed(title = "Test List Feed", podcasts = testDiscoverPodcasts, subtitle = null, description = null, date = null, episodes = null, collectionImageUrl = null, featureImage = null, headerImageUrl = null, tintColors = null, collageImages = null, webLinkUrl = null, webLinkTitle = null, promotion = null)
    }

    @Test
    fun testFeatureFlagDisabledNoPodcastsFetched() {
        // feature flag is disabled
        FeatureFlag.setEnabled(feature = Feature.RECOMMENDATIONS, enabled = false)

        similarPodcasts.setEnabled(true)

        val podcasts = similarPodcasts.getSimilarPodcastsFlowable(testPodcast)
            .test()
            .awaitCount(1)
            .assertNoErrors()
            .values()
            .first()

        // no podcasts should be found
        assert(podcasts.isEmpty())
    }

    @Test
    fun testEnabledAndFeatureFlagOnPodcastsFetched() = runTest {
        FeatureFlag.setEnabled(feature = Feature.RECOMMENDATIONS, enabled = true)
        similarPodcasts.setEnabled(true)

        // expected URL for the list recommendation
        val listUrl = "${Settings.SERVER_API_URL}/recommendations/podcast?podcast_uuid=${testPodcast.uuid}"
        whenever(listWebService.getListFeed(listUrl)).thenReturn(testListFeed)

        // mark the first podcast as subscribed
        val subscribedUuid = testDiscoverPodcasts.first().uuid
        whenever(podcastManager.getSubscribedPodcastUuidsRxSingle()).thenReturn(Single.just(listOf(subscribedUuid)))
        whenever(podcastManager.podcastSubscriptionsRxFlowable()).thenReturn(Flowable.just(emptyList()))

        // call the method to test
        val podcasts = similarPodcasts.getSimilarPodcastsFlowable(testPodcast)
            .test()
            .awaitCount(1)
            .assertNoErrors()
            .values()
            .first()

        // check that the podcasts are fetched correctly
        assertEquals(testListFeed.podcasts?.size, podcasts.size)

        // check that the subscribed status is set correctly
        val subscribedPodcast = podcasts.find { it.uuid == subscribedUuid }
        val unsubscribedPodcast = podcasts.find { it.uuid != subscribedUuid }

        assert(subscribedPodcast?.isSubscribed == true)
        assert(unsubscribedPodcast?.isSubscribed == false)
    }
}
