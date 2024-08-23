package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.entity.CuratedPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherRecentlyPlayedPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherSubscribedPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherTrendingPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PodcastDaoTest {
    lateinit var podcastDao: PodcastDao
    lateinit var episodeDao: EpisodeDao
    lateinit var testDb: AppDatabase

    @Before
    fun setupDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        podcastDao = testDb.podcastDao()
        episodeDao = testDb.episodeDao()
    }

    @After
    fun closeDb() {
        testDb.close()
    }

    @Test
    fun testInsertPodcast() {
        val uuid = UUID.randomUUID().toString()
        podcastDao.insert(Podcast(uuid = uuid, isSubscribed = true))
        assertNotNull("Inserted podcast should be able to be found", podcastDao.findByUuid(uuid))
    }

    @Test
    fun testInsertPodcastRx() {
        val uuid = UUID.randomUUID().toString()
        podcastDao.insertRx(Podcast(uuid = uuid)).blockingGet()
        assertNotNull("Inserted podcast should be able to be found", podcastDao.findByUuid(uuid))
    }

    @Test
    fun testInsertMultiple() {
        val uuid = UUID.randomUUID().toString()
        val podcast = Podcast(uuid = uuid, isSubscribed = false)
        podcastDao.insert(podcast)
        val podcast2 = Podcast(uuid = uuid, isSubscribed = true)
        podcastDao.insert(podcast2)

        assertEquals("Insert should replace, count should be 1", 1, podcastDao.countByUuid(uuid))
        assertEquals("Podcast should be replaced, should be subscribed", true, podcastDao.findByUuid(uuid)?.isSubscribed)
    }

    @Test
    fun testUpdatePodcast() {
        val uuid = UUID.randomUUID().toString()
        val podcast = Podcast(uuid = uuid, isSubscribed = false)
        podcastDao.insert(podcast)
        assert(podcastDao.findByUuid(uuid)?.isSubscribed == false)
        podcast.isSubscribed = true
        podcastDao.update(podcast)
        assertTrue("Podcast should be updated to subscribed", podcastDao.findByUuid(uuid)?.isSubscribed == true)
    }

    @Test
    fun testUpdatePodcastRx() {
        val uuid = UUID.randomUUID().toString()
        val podcast = Podcast(uuid = uuid, isSubscribed = false)
        podcastDao.insert(podcast)
        assert(podcastDao.findByUuid(uuid)?.isSubscribed == false)
        podcast.isSubscribed = true
        podcastDao.updateRx(podcast).blockingAwait()
        assertTrue("Podcast should be updated to subscribed", podcastDao.findByUuid(uuid)?.isSubscribed == true)
    }

    @Test
    fun testFindSubscribed() {
        val subscribed = Podcast(uuid = UUID.randomUUID().toString(), isSubscribed = true)
        val unsubscribed = Podcast(uuid = UUID.randomUUID().toString(), isSubscribed = false)
        podcastDao.insert(subscribed)
        podcastDao.insert(unsubscribed)

        val subscribedList = podcastDao.findSubscribed()
        assertEquals("Should only be 1 result", 1, subscribedList.count())
        assertEquals("Should only find the subscribed podcast", subscribed.uuid, subscribedList.first().uuid)
    }

    @Test
    fun testFindSubscribedRx() {
        val subscribed = Podcast(uuid = UUID.randomUUID().toString(), isSubscribed = true)
        val unsubscribed = Podcast(uuid = UUID.randomUUID().toString(), isSubscribed = false)
        podcastDao.insert(subscribed)
        podcastDao.insert(unsubscribed)

        val subscribedList = podcastDao.findSubscribedRx().blockingGet()
        assertEquals("Should only be 1 result", 1, subscribedList.count())
        assertEquals("Should only find the subscribed podcast", subscribed.uuid, subscribedList.first().uuid)
    }

    @Test
    fun useCorrectReleaseTimestampsForNovaLauncherSubscribedPodcasts() = runTest {
        podcastDao.insert(Podcast(uuid = "id-1", title = "title-1", isSubscribed = true))
        episodeDao.insert(PodcastEpisode(uuid = "id-1", publishedDate = Date(0), podcastUuid = "id-1", lastPlaybackInteraction = 10))
        episodeDao.insert(PodcastEpisode(uuid = "id-2", publishedDate = Date(2), podcastUuid = "id-1", lastPlaybackInteraction = null))
        episodeDao.insert(PodcastEpisode(uuid = "id-3", publishedDate = Date(1), podcastUuid = "id-1", lastPlaybackInteraction = 13))

        podcastDao.insert(Podcast(uuid = "id-2", title = "title-2", isSubscribed = true))
        episodeDao.insert(PodcastEpisode(uuid = "id-4", publishedDate = Date(5), podcastUuid = "id-2", lastPlaybackInteraction = 0))
        episodeDao.insert(PodcastEpisode(uuid = "id-5", publishedDate = Date(4), podcastUuid = "id-2", lastPlaybackInteraction = 20))
        episodeDao.insert(PodcastEpisode(uuid = "id-6", publishedDate = Date(3), podcastUuid = "id-2", lastPlaybackInteraction = 25))

        podcastDao.insert(Podcast(uuid = "id-3", title = "title-3", isSubscribed = true))
        episodeDao.insert(PodcastEpisode(uuid = "id-7", publishedDate = Date(12), podcastUuid = "id-3", lastPlaybackInteraction = 0))

        podcastDao.insert(Podcast(uuid = "id-4", title = "title-4", isSubscribed = true))

        val podcasts = podcastDao.getNovaLauncherSubscribedPodcasts(PodcastsSortType.NAME_A_TO_Z, limit = 100)

        val expected = listOf(
            NovaLauncherSubscribedPodcast(
                id = "id-1",
                title = "title-1",
                categories = "",
                initialReleaseTimestamp = 0,
                latestReleaseTimestamp = 2,
                lastUsedTimestamp = 13,
            ),
            NovaLauncherSubscribedPodcast(
                id = "id-2",
                title = "title-2",
                categories = "",
                initialReleaseTimestamp = 3,
                latestReleaseTimestamp = 5,
                lastUsedTimestamp = 25,
            ),
            NovaLauncherSubscribedPodcast(
                id = "id-3",
                title = "title-3",
                categories = "",
                initialReleaseTimestamp = 12,
                latestReleaseTimestamp = 12,
                lastUsedTimestamp = 0,
            ),
            NovaLauncherSubscribedPodcast(
                id = "id-4",
                title = "title-4",
                categories = "",
                initialReleaseTimestamp = null,
                latestReleaseTimestamp = null,
                lastUsedTimestamp = null,
            ),
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun includeOnlySubscribedPodcastsForNovaLauncherSubscribedPodcasts() = runTest {
        podcastDao.insert(Podcast(uuid = "id-1", title = "title-1", isSubscribed = true))
        episodeDao.insert(PodcastEpisode(uuid = "id-1", publishedDate = Date(0), podcastUuid = "id-1"))
        episodeDao.insert(PodcastEpisode(uuid = "id-2", publishedDate = Date(0), podcastUuid = "id-1"))
        episodeDao.insert(PodcastEpisode(uuid = "id-3", publishedDate = Date(0), podcastUuid = "id-1"))

        podcastDao.insert(Podcast(uuid = "id-2", title = "title-2", isSubscribed = false))
        episodeDao.insert(PodcastEpisode(uuid = "id-4", publishedDate = Date(0), podcastUuid = "id-2"))
        episodeDao.insert(PodcastEpisode(uuid = "id-5", publishedDate = Date(0), podcastUuid = "id-2"))
        episodeDao.insert(PodcastEpisode(uuid = "id-6", publishedDate = Date(0), podcastUuid = "id-2"))

        val podcasts = podcastDao.getNovaLauncherSubscribedPodcasts(PodcastsSortType.NAME_A_TO_Z, limit = 100)

        val expected = listOf(
            NovaLauncherSubscribedPodcast(
                id = "id-1",
                title = "title-1",
                categories = "",
                initialReleaseTimestamp = 0,
                latestReleaseTimestamp = 0,
                lastUsedTimestamp = null,
            ),
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun includeCategoriesForNovaLauncherSubscribedPodcasts() = runTest {
        podcastDao.insert(Podcast(uuid = "id-1", title = "title-1", isSubscribed = true, podcastCategory = "category-1"))

        val podcasts = podcastDao.getNovaLauncherSubscribedPodcasts(PodcastsSortType.NAME_A_TO_Z, limit = 100)

        val expected = listOf(
            NovaLauncherSubscribedPodcast(
                id = "id-1",
                title = "title-1",
                categories = "category-1",
                initialReleaseTimestamp = null,
                latestReleaseTimestamp = null,
                lastUsedTimestamp = null,
            ),
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun sortNovaLauncherSubscribedPodcastsByAddedDate() = runTest {
        podcastDao.insert(Podcast(uuid = "id-1", title = "title-1", isSubscribed = true, addedDate = Date(3)))
        podcastDao.insert(Podcast(uuid = "id-2", title = "title-2", isSubscribed = true, addedDate = Date(4)))
        podcastDao.insert(Podcast(uuid = "id-3", title = "title-3", isSubscribed = true, addedDate = Date(2)))
        podcastDao.insert(Podcast(uuid = "id-4", title = "title-4", isSubscribed = true, addedDate = Date(1)))
        podcastDao.insert(Podcast(uuid = "id-5", title = "title-5", isSubscribed = true, addedDate = null))

        val podcastIds = podcastDao.getNovaLauncherSubscribedPodcasts(PodcastsSortType.DATE_ADDED_OLDEST_TO_NEWEST, limit = 100).map { it.id }

        assertEquals(listOf("id-4", "id-3", "id-1", "id-2", "id-5"), podcastIds)
    }

    @Test
    fun sortNovaLauncherSubscribedPodcastsByTitle() = runTest {
        podcastDao.insert(Podcast(uuid = "id-1", title = "title-4", isSubscribed = true))
        podcastDao.insert(Podcast(uuid = "id-2", title = "title-3", isSubscribed = true))
        podcastDao.insert(Podcast(uuid = "id-3", title = "title-1", isSubscribed = true))
        podcastDao.insert(Podcast(uuid = "id-4", title = "title-2", isSubscribed = true))

        val podcastIds = podcastDao.getNovaLauncherSubscribedPodcasts(PodcastsSortType.NAME_A_TO_Z, limit = 100).map { it.id }

        assertEquals(listOf("id-3", "id-4", "id-2", "id-1"), podcastIds)
    }

    @Test
    fun sortingNovaLauncherSubscribedPodcastsByTitleOmitsEnglishArticles() = runTest {
        podcastDao.insert(Podcast(uuid = "id-1", title = "The 1", isSubscribed = true))
        podcastDao.insert(Podcast(uuid = "id-2", title = "An 2", isSubscribed = true))
        podcastDao.insert(Podcast(uuid = "id-3", title = "A 3", isSubscribed = true))
        podcastDao.insert(Podcast(uuid = "id-4", title = "4", isSubscribed = true))
        podcastDao.insert(Podcast(uuid = "id-5", title = "the 5", isSubscribed = true))
        podcastDao.insert(Podcast(uuid = "id-6", title = "an 6", isSubscribed = true))
        podcastDao.insert(Podcast(uuid = "id-7", title = "a 7", isSubscribed = true))
        podcastDao.insert(Podcast(uuid = "id-8", title = "8", isSubscribed = true))

        val podcastIds = podcastDao.getNovaLauncherSubscribedPodcasts(PodcastsSortType.NAME_A_TO_Z, limit = 100).map { it.id }

        assertEquals(listOf("id-1", "id-2", "id-3", "id-4", "id-5", "id-6", "id-7", "id-8"), podcastIds)
    }

    @Test
    fun sortNovaLauncherSubscribedPodcastsByLatestReleaseTimestamp() = runTest {
        podcastDao.insert(Podcast(uuid = "id-1", title = "title-1", isSubscribed = true))
        episodeDao.insert(PodcastEpisode(uuid = "id-1", publishedDate = Date(0), podcastUuid = "id-1"))

        podcastDao.insert(Podcast(uuid = "id-2", title = "title-2", isSubscribed = true))
        episodeDao.insert(PodcastEpisode(uuid = "id-2", publishedDate = Date(100), podcastUuid = "id-2"))

        podcastDao.insert(Podcast(uuid = "id-3", title = "title-3", isSubscribed = true))

        podcastDao.insert(Podcast(uuid = "id-4", title = "title-4", isSubscribed = true))
        episodeDao.insert(PodcastEpisode(uuid = "id-3", publishedDate = Date(200), podcastUuid = "id-4"))

        val podcastIds = podcastDao.getNovaLauncherSubscribedPodcasts(PodcastsSortType.EPISODE_DATE_NEWEST_TO_OLDEST, limit = 100).map { it.id }

        assertEquals(listOf("id-4", "id-2", "id-1", "id-3"), podcastIds)
    }

    @Test
    fun sortNovaLauncherSubscribedPodcastsByCustomSortOrder() = runTest {
        podcastDao.insert(Podcast(uuid = "id-1", title = "title-1", isSubscribed = true, sortPosition = 3))
        podcastDao.insert(Podcast(uuid = "id-2", title = "title-2", isSubscribed = true, sortPosition = 1))
        podcastDao.insert(Podcast(uuid = "id-3", title = "title-3", isSubscribed = true, sortPosition = 2))
        podcastDao.insert(Podcast(uuid = "id-4", title = "title-4", isSubscribed = true, sortPosition = 4))

        val podcastIds = podcastDao.getNovaLauncherSubscribedPodcasts(PodcastsSortType.DRAG_DROP, limit = 100).map { it.id }

        assertEquals(listOf("id-2", "id-3", "id-1", "id-4"), podcastIds)
    }

    @Test
    fun limitNovaLauncherSubscribedPodcasts() = runTest {
        List(250) {
            podcastDao.insert(Podcast(uuid = "id-$it", isSubscribed = true))
        }

        val podcasts = podcastDao.getNovaLauncherSubscribedPodcasts(PodcastsSortType.NAME_A_TO_Z, limit = 60)

        assertEquals(60, podcasts.size)
    }

    @Test
    fun getAllTrendingPodcastsForNovaLauncher() = runTest {
        val curatedPodcasts = List(65) {
            CuratedPodcast(
                listId = "trending",
                listTitle = "Trending",
                podcastId = "id-$it",
                podcastTitle = "title-$it",
                podcastDescription = null,
            )
        }
        podcastDao.replaceAllCuratedPodcasts(curatedPodcasts)

        val podcasts = podcastDao.getNovaLauncherTrendingPodcasts(limit = 100)

        val expected = List(65) {
            NovaLauncherTrendingPodcast("id-$it", "title-$it")
        }
        assertEquals(expected, podcasts)
    }

    @Test
    fun limitTrendingPodcastsForNovaLauncher() = runTest {
        val trendingPodcasts = List(65) {
            CuratedPodcast(
                listId = "trending",
                listTitle = "Trending",
                podcastId = "id-$it",
                podcastTitle = "title-$it",
                podcastDescription = null,
            )
        }
        podcastDao.replaceAllCuratedPodcasts(trendingPodcasts)

        val podcasts = podcastDao.getNovaLauncherTrendingPodcasts(limit = 5)

        assertEquals(5, podcasts.size)
    }

    @Test
    fun doNotIncludeTrendingPodcastsThatAreSubscribedForNovaLauncher() = runTest {
        val trendingPodcasts = List(3) {
            CuratedPodcast(
                listId = "trending",
                listTitle = "Trending",
                podcastId = "id-$it",
                podcastTitle = "title-$it",
                podcastDescription = null,
            )
        }
        podcastDao.replaceAllCuratedPodcasts(trendingPodcasts)
        podcastDao.insert(Podcast("id-0", isSubscribed = true))
        podcastDao.insert(Podcast("id-1", isSubscribed = false))

        val podcasts = podcastDao.getNovaLauncherTrendingPodcasts(limit = 100)

        val expected = listOf(
            NovaLauncherTrendingPodcast("id-1", "title-1"),
            NovaLauncherTrendingPodcast("id-2", "title-2"),
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun getOnlyTrendingPodcastsForNovaLauncher() = runTest {
        val curatedPodcasts = listOf(
            CuratedPodcast(
                listId = "trending",
                listTitle = "Trending",
                podcastId = "id-0",
                podcastTitle = "title-0",
                podcastDescription = null,
            ),
            CuratedPodcast(
                listId = "featured",
                listTitle = "Featured",
                podcastId = "id-1",
                podcastTitle = "title-1",
                podcastDescription = null,
            ),
            CuratedPodcast(
                listId = "some-other-list",
                listTitle = "Some other list",
                podcastId = "id-2",
                podcastTitle = "title-2",
                podcastDescription = null,
            ),
        )
        podcastDao.replaceAllCuratedPodcasts(curatedPodcasts)

        val podcasts = podcastDao.getNovaLauncherTrendingPodcasts(limit = 100)

        val expected = listOf(
            NovaLauncherTrendingPodcast("id-0", "title-0"),
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun useCorrectReleaseTimestampsForNovaLauncherRecentlyPlayedPodcasts() = runTest {
        val interactionDate1 = Date.from(Instant.now().minus(1, ChronoUnit.DAYS))
        val interactionDate2 = Date.from(Instant.now().minus(2, ChronoUnit.DAYS))
        val interactionDate3 = Date.from(Instant.now().minus(3, ChronoUnit.DAYS))
        podcastDao.insert(Podcast(uuid = "id-1", title = "title-1", isSubscribed = true))
        episodeDao.insert(PodcastEpisode(uuid = "id-1", publishedDate = Date(0), podcastUuid = "id-1", lastPlaybackInteraction = interactionDate1.time))
        episodeDao.insert(PodcastEpisode(uuid = "id-2", publishedDate = Date(2), podcastUuid = "id-1", lastPlaybackInteraction = null))
        episodeDao.insert(PodcastEpisode(uuid = "id-3", publishedDate = Date(1), podcastUuid = "id-1", lastPlaybackInteraction = 13000))

        podcastDao.insert(Podcast(uuid = "id-2", title = "title-2", isSubscribed = false))
        episodeDao.insert(PodcastEpisode(uuid = "id-4", publishedDate = Date(5), podcastUuid = "id-2", lastPlaybackInteraction = 0))
        episodeDao.insert(PodcastEpisode(uuid = "id-5", publishedDate = Date(4), podcastUuid = "id-2", lastPlaybackInteraction = 20))
        episodeDao.insert(PodcastEpisode(uuid = "id-6", publishedDate = Date(3), podcastUuid = "id-2", lastPlaybackInteraction = interactionDate2.time))

        podcastDao.insert(Podcast(uuid = "id-3", title = "title-3", isSubscribed = true))
        episodeDao.insert(PodcastEpisode(uuid = "id-7", publishedDate = Date(12), podcastUuid = "id-3", lastPlaybackInteraction = interactionDate3.time))

        val podcasts = podcastDao.getNovaLauncherRecentlyPlayedPodcasts(limit = 100)

        val expected = listOf(
            NovaLauncherRecentlyPlayedPodcast(
                id = "id-1",
                title = "title-1",
                categories = "",
                initialReleaseTimestamp = 0,
                latestReleaseTimestamp = 2,
                lastUsedTimestamp = interactionDate1.time,
            ),
            NovaLauncherRecentlyPlayedPodcast(
                id = "id-2",
                title = "title-2",
                categories = "",
                initialReleaseTimestamp = 3,
                latestReleaseTimestamp = 5,
                lastUsedTimestamp = interactionDate2.time,
            ),
            NovaLauncherRecentlyPlayedPodcast(
                id = "id-3",
                title = "title-3",
                categories = "",
                initialReleaseTimestamp = 12,
                latestReleaseTimestamp = 12,
                lastUsedTimestamp = interactionDate3.time,
            ),
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun includeOnlyInteractedWithPodcastsForNovaLauncherRecentlyPlayedPodcasts() = runTest {
        val interactionDate = Date()
        podcastDao.insert(Podcast(uuid = "id-1", title = "title-1"))
        episodeDao.insert(PodcastEpisode(uuid = "id-1", publishedDate = Date(0), podcastUuid = "id-1", lastPlaybackInteraction = interactionDate.time))

        podcastDao.insert(Podcast(uuid = "id-2", title = "title-2"))
        episodeDao.insert(PodcastEpisode(uuid = "id-2", publishedDate = Date(0), podcastUuid = "id-2", lastPlaybackInteraction = null))

        podcastDao.insert(Podcast(uuid = "id-3", title = "title-3"))

        val podcasts = podcastDao.getNovaLauncherRecentlyPlayedPodcasts(limit = 100)

        val expected = listOf(
            NovaLauncherRecentlyPlayedPodcast(
                id = "id-1",
                title = "title-1",
                categories = "",
                initialReleaseTimestamp = 0,
                latestReleaseTimestamp = 0,
                lastUsedTimestamp = interactionDate.time,
            ),
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun getRecentlyPlayedPodcastsForNovaLauncherSortedByInteractionDate() = runTest {
        val interactionDate1 = Date.from(Instant.now().minus(3, ChronoUnit.DAYS))
        val interactionDate2 = Date.from(Instant.now().minus(1, ChronoUnit.DAYS))
        val interactionDate3 = Date.from(Instant.now().minus(2, ChronoUnit.DAYS))
        podcastDao.insert(Podcast(uuid = "id-1", title = "title-1"))
        episodeDao.insert(PodcastEpisode(uuid = "id-1", publishedDate = Date(0), podcastUuid = "id-1", lastPlaybackInteraction = interactionDate1.time))

        podcastDao.insert(Podcast(uuid = "id-2", title = "title-2"))
        episodeDao.insert(PodcastEpisode(uuid = "id-2", publishedDate = Date(0), podcastUuid = "id-2", lastPlaybackInteraction = interactionDate2.time))

        podcastDao.insert(Podcast(uuid = "id-3", title = "title-3"))
        episodeDao.insert(PodcastEpisode(uuid = "id-3", publishedDate = Date(0), podcastUuid = "id-3", lastPlaybackInteraction = interactionDate3.time))

        val podcasts = podcastDao.getNovaLauncherRecentlyPlayedPodcasts(limit = 100)

        val expected = listOf(
            NovaLauncherRecentlyPlayedPodcast(
                id = "id-2",
                title = "title-2",
                categories = "",
                initialReleaseTimestamp = 0,
                latestReleaseTimestamp = 0,
                lastUsedTimestamp = interactionDate2.time,
            ),
            NovaLauncherRecentlyPlayedPodcast(
                id = "id-3",
                title = "title-3",
                categories = "",
                initialReleaseTimestamp = 0,
                latestReleaseTimestamp = 0,
                lastUsedTimestamp = interactionDate3.time,
            ),
            NovaLauncherRecentlyPlayedPodcast(
                id = "id-1",
                title = "title-1",
                categories = "",
                initialReleaseTimestamp = 0,
                latestReleaseTimestamp = 0,
                lastUsedTimestamp = interactionDate1.time,
            ),
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun limitNovaLauncherRecentlyPlayedPodcasts() = runTest {
        List(250) {
            podcastDao.insert(Podcast(uuid = "id-$it"))
            episodeDao.insert(PodcastEpisode(uuid = "id-$it", podcastUuid = "id-$it", publishedDate = Date(), lastPlaybackInteraction = Date().time))
        }

        val inProgressEpisodes = podcastDao.getNovaLauncherRecentlyPlayedPodcasts(limit = 100)

        assertEquals(100, inProgressEpisodes.size)
    }

    @Test
    fun ignoreRecentlyPlayedPodcastsForNovaLauncherThatAreAtLeast60DaysOld() = runTest {
        val interactionDate1 = Date.from(Instant.now().minus(61, ChronoUnit.DAYS))
        val interactionDate2 = Date.from(Instant.now().minus(59, ChronoUnit.DAYS))
        podcastDao.insert(Podcast(uuid = "id-1", title = "title-1", isSubscribed = true))
        episodeDao.insert(PodcastEpisode(uuid = "id-1", publishedDate = Date(0), podcastUuid = "id-1", lastPlaybackInteraction = interactionDate1.time))

        podcastDao.insert(Podcast(uuid = "id-2", title = "title-2", isSubscribed = false))
        episodeDao.insert(PodcastEpisode(uuid = "id-2", publishedDate = Date(0), podcastUuid = "id-2", lastPlaybackInteraction = interactionDate2.time))

        val podcasts = podcastDao.getNovaLauncherRecentlyPlayedPodcasts(limit = 100)

        val expected = listOf(
            NovaLauncherRecentlyPlayedPodcast(
                id = "id-2",
                title = "title-2",
                categories = "",
                initialReleaseTimestamp = 0,
                latestReleaseTimestamp = 0,
                lastUsedTimestamp = interactionDate2.time,
            ),
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun includeCategoriesForNovaLauncherRecentlyPlayedPodcasts() = runTest {
        val interactionDate1 = Date.from(Instant.now().minus(1, ChronoUnit.DAYS))
        podcastDao.insert(Podcast(uuid = "id-1", title = "title-1", podcastCategory = "category1"))
        episodeDao.insert(PodcastEpisode(uuid = "id-1", publishedDate = Date(0), podcastUuid = "id-1", lastPlaybackInteraction = interactionDate1.time))

        val podcasts = podcastDao.getNovaLauncherRecentlyPlayedPodcasts(limit = 100)

        val expected = listOf(
            NovaLauncherRecentlyPlayedPodcast(
                id = "id-1",
                title = "title-1",
                categories = "category1",
                initialReleaseTimestamp = 0,
                latestReleaseTimestamp = 0,
                lastUsedTimestamp = interactionDate1.time,
            ),
        )
        assertEquals(expected, podcasts)
    }
}
