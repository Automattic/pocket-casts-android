package au.com.shiftyjelly.pocketcasts.discover

import android.content.Context
import android.content.res.Resources
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.servers.model.Discover
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverEpisode
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRegion
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRow
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TvDiscoverFeedLoaderTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val listRepository = mock<ListRepository>()
    private val settings = mock<Settings>()
    private val context = mock<Context>()

    private fun TestScope.loader() = TvDiscoverFeedLoader(
        listRepository = listRepository,
        settings = settings,
        applicationScope = backgroundScope,
        context = context,
    )

    @Test
    fun `builds first and last cover urls and caches them after one fetch`() = runTest {
        val feed = feedOf("a", "b", "c")
        whenever(listRepository.getListFeed(eq(SOURCE), any())).doReturn(feed)
        val loader = loader()

        val first = loader.loadCategoryCoverUrls(SOURCE)
        val second = loader.loadCategoryCoverUrls(SOURCE)

        assertEquals(listOf(artwork("a"), artwork("c")), first)
        assertEquals(first, second)
        verify(listRepository, times(1)).getListFeed(eq(SOURCE), any())
    }

    @Test
    fun `does not cache a failed feed load so it can be retried`() = runTest {
        val feed = feedOf("a", "b")
        whenever(listRepository.getListFeed(eq(SOURCE), any())).doReturn(null, feed)
        val loader = loader()

        val first = loader.loadCategoryCoverUrls(SOURCE)
        val second = loader.loadCategoryCoverUrls(SOURCE)

        assertEquals(emptyList<String>(), first)
        assertEquals(listOf(artwork("a"), artwork("b")), second)
        verify(listRepository, times(2)).getListFeed(eq(SOURCE), any())
    }

    @Test
    fun `a cancelled caller does not stop the covers from loading`() = runTest {
        val gate = CompletableDeferred<ListFeed>()
        whenever(listRepository.getListFeed(eq(SOURCE), any())).doSuspendableAnswer { gate.await() }
        val loader = loader()

        val cancelled = launch { loader.loadCategoryCoverUrls(SOURCE) }
        runCurrent()
        cancelled.cancel()
        gate.complete(feedOf("a", "b"))
        advanceUntilIdle()

        assertEquals(listOf(artwork("a"), artwork("b")), loader.loadCategoryCoverUrls(SOURCE))
        verify(listRepository, times(1)).getListFeed(eq(SOURCE), any())
    }

    @Test
    fun `concurrent callers for the same source share one fetch`() = runTest {
        val gate = CompletableDeferred<ListFeed>()
        whenever(listRepository.getListFeed(eq(SOURCE), any())).doSuspendableAnswer { gate.await() }
        val loader = loader()

        val first = async { loader.loadCategoryCoverUrls(SOURCE) }
        val second = async { loader.loadCategoryCoverUrls(SOURCE) }
        runCurrent()
        gate.complete(feedOf("a", "b"))

        assertEquals(listOf(artwork("a"), artwork("b")), first.await())
        assertEquals(first.await(), second.await())
        verify(listRepository, times(1)).getListFeed(eq(SOURCE), any())
    }

    @Test
    fun `video preview list episodes carry a preview url`() = runTest {
        stubCountry()
        val feed = mock<ListFeed> { on { episodes } doReturn listOf(videoEpisode()) }
        whenever(listRepository.getListFeed(eq(SOURCE), any())).doReturn(feed)

        val rows = loader().buildRows(discoverWith(DisplayStyle.VideoPreviewList()), isLoggedIn = true)

        val episodes = (rows.single() as TvDiscoverRow.Episodes).episodes
        assertEquals("https://example.com/video.mp4", episodes.single().videoPreviewUrl)
    }

    @Test
    fun `non video preview episode lists do not carry a preview url`() = runTest {
        stubCountry()
        val feed = mock<ListFeed> { on { episodes } doReturn listOf(videoEpisode()) }
        whenever(listRepository.getListFeed(eq(SOURCE), any())).doReturn(feed)

        val rows = loader().buildRows(discoverWith(DisplayStyle.SmallList()), isLoggedIn = true)

        val episodes = (rows.single() as TvDiscoverRow.Episodes).episodes
        assertEquals(null, episodes.single().videoPreviewUrl)
    }

    private fun stubCountry() {
        val countrySetting = mock<UserSetting<String>> { on { value } doReturn REGION }
        whenever(settings.discoverCountryCode).doReturn(countrySetting)
        whenever(context.resources).doReturn(mock<Resources>())
    }

    private fun discoverWith(displayStyle: DisplayStyle) = Discover(
        layout = listOf(
            DiscoverRow(
                id = "made-for-tv",
                type = ListType.EpisodeList,
                displayStyle = displayStyle,
                expandedTopItemLabel = null,
                title = "Made for TV",
                source = SOURCE,
                listUuid = null,
                categoryId = null,
                regions = listOf(REGION),
                mostPopularCategoriesId = null,
                sponsoredCategoryIds = null,
            ),
        ),
        regions = mapOf(REGION to DiscoverRegion(name = "Region", flag = "flag", code = REGION)),
        regionCodeToken = "[regionCode]",
        regionNameToken = "[regionName]",
        defaultRegionCode = REGION,
    )

    private fun videoEpisode() = DiscoverEpisode(
        uuid = "episode-uuid",
        title = "Episode",
        url = "https://example.com/video.mp4",
        published = null,
        duration = null,
        fileType = "video/mp4",
        size = null,
        podcast_uuid = "podcast-uuid",
        podcast_title = "Podcast",
        type = null,
        season = null,
        number = null,
    )

    private fun feedOf(vararg uuids: String) = mock<ListFeed> {
        on { podcasts } doReturn uuids.map { uuid ->
            DiscoverPodcast(
                uuid = uuid,
                title = null,
                url = null,
                author = null,
                category = null,
                description = null,
                language = null,
                mediaType = null,
            )
        }
    }

    private fun artwork(uuid: String) = PodcastImage.getArtworkUrl(size = 200, uuid = uuid, isWearOS = false)

    private companion object {
        const val SOURCE = "https://category/us.json"
        const val REGION = "zz"
    }
}
