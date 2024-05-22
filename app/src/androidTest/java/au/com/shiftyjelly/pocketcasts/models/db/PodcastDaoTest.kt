package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherRecentlyPlayedPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherSubscribedPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherTrendingPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.TrendingPodcast
import au.com.shiftyjelly.pocketcasts.utils.extensions.timeSecs
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
        episodeDao.insert(PodcastEpisode(uuid = "id-1", publishedDate = Date(0), podcastUuid = "id-1", lastPlaybackInteraction = 10000))
        episodeDao.insert(PodcastEpisode(uuid = "id-2", publishedDate = Date(2000), podcastUuid = "id-1", lastPlaybackInteraction = null))
        episodeDao.insert(PodcastEpisode(uuid = "id-3", publishedDate = Date(1000), podcastUuid = "id-1", lastPlaybackInteraction = 13000))

        podcastDao.insert(Podcast(uuid = "id-2", title = "title-2", isSubscribed = true))
        episodeDao.insert(PodcastEpisode(uuid = "id-4", publishedDate = Date(5000), podcastUuid = "id-2", lastPlaybackInteraction = 0))
        episodeDao.insert(PodcastEpisode(uuid = "id-5", publishedDate = Date(4000), podcastUuid = "id-2", lastPlaybackInteraction = 20000))
        episodeDao.insert(PodcastEpisode(uuid = "id-6", publishedDate = Date(3000), podcastUuid = "id-2", lastPlaybackInteraction = 25000))

        podcastDao.insert(Podcast(uuid = "id-3", title = "title-3", isSubscribed = true))
        episodeDao.insert(PodcastEpisode(uuid = "id-7", publishedDate = Date(12000), podcastUuid = "id-3", lastPlaybackInteraction = 0))

        podcastDao.insert(Podcast(uuid = "id-4", title = "title-4", isSubscribed = true))

        val podcasts = podcastDao.getNovaLauncherSubscribedPodcasts()

        val expected = listOf(
            NovaLauncherSubscribedPodcast(
                id = "id-1",
                title = "title-1",
                initialReleaseTimestamp = 0,
                latestReleaseTimestamp = 2,
                lastUsedTimestamp = 13,
            ),
            NovaLauncherSubscribedPodcast(
                id = "id-2",
                title = "title-2",
                initialReleaseTimestamp = 3,
                latestReleaseTimestamp = 5,
                lastUsedTimestamp = 25,
            ),
            NovaLauncherSubscribedPodcast(
                id = "id-3",
                title = "title-3",
                initialReleaseTimestamp = 12,
                latestReleaseTimestamp = 12,
                lastUsedTimestamp = 0,
            ),
            NovaLauncherSubscribedPodcast(
                id = "id-4",
                title = "title-4",
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

        val podcasts = podcastDao.getNovaLauncherSubscribedPodcasts()

        val expected = listOf(
            NovaLauncherSubscribedPodcast(
                id = "id-1",
                title = "title-1",
                initialReleaseTimestamp = 0,
                latestReleaseTimestamp = 0,
                lastUsedTimestamp = null,
            ),
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun getAllTrendingPodcastsForNovaLauncher() = runTest {
        val trendingPodcasts = List(65) {
            TrendingPodcast("id-$it", "title-$it")
        }
        podcastDao.replaceAllTrendingPodcasts(trendingPodcasts)

        val podcasts = podcastDao.getNovaLauncherTrendingPodcasts()

        val expected = List(65) {
            NovaLauncherTrendingPodcast("id-$it", "title-$it")
        }
        assertEquals(expected, podcasts)
    }

    @Test
    fun doNotIncludeTrendingPodcastsThatAreTrendingForNovaLauncher() = runTest {
        val trendingPodcasts = listOf(
            TrendingPodcast("id-1", "title-1"),
            TrendingPodcast("id-2", "title-2"),
            TrendingPodcast("id-3", "title-3"),
        )
        podcastDao.replaceAllTrendingPodcasts(trendingPodcasts)
        podcastDao.insert(Podcast("id-1", isSubscribed = true))
        podcastDao.insert(Podcast("id-2", isSubscribed = false))

        val podcasts = podcastDao.getNovaLauncherTrendingPodcasts()

        val expected = listOf(
            NovaLauncherTrendingPodcast("id-2", "title-2"),
            NovaLauncherTrendingPodcast("id-3", "title-3"),
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
        episodeDao.insert(PodcastEpisode(uuid = "id-2", publishedDate = Date(2000), podcastUuid = "id-1", lastPlaybackInteraction = null))
        episodeDao.insert(PodcastEpisode(uuid = "id-3", publishedDate = Date(1000), podcastUuid = "id-1", lastPlaybackInteraction = 13000))

        podcastDao.insert(Podcast(uuid = "id-2", title = "title-2", isSubscribed = false))
        episodeDao.insert(PodcastEpisode(uuid = "id-4", publishedDate = Date(5000), podcastUuid = "id-2", lastPlaybackInteraction = 0))
        episodeDao.insert(PodcastEpisode(uuid = "id-5", publishedDate = Date(4000), podcastUuid = "id-2", lastPlaybackInteraction = 20000))
        episodeDao.insert(PodcastEpisode(uuid = "id-6", publishedDate = Date(3000), podcastUuid = "id-2", lastPlaybackInteraction = interactionDate2.time))

        podcastDao.insert(Podcast(uuid = "id-3", title = "title-3", isSubscribed = true))
        episodeDao.insert(PodcastEpisode(uuid = "id-7", publishedDate = Date(12000), podcastUuid = "id-3", lastPlaybackInteraction = interactionDate3.time))

        val podcasts = podcastDao.getNovaLauncherRecentlyPlayedPodcasts()

        val expected = listOf(
            NovaLauncherRecentlyPlayedPodcast(
                id = "id-1",
                title = "title-1",
                initialReleaseTimestamp = 0,
                latestReleaseTimestamp = 2,
                lastUsedTimestamp = interactionDate1.timeSecs(),
            ),
            NovaLauncherRecentlyPlayedPodcast(
                id = "id-2",
                title = "title-2",
                initialReleaseTimestamp = 3,
                latestReleaseTimestamp = 5,
                lastUsedTimestamp = interactionDate2.timeSecs(),
            ),
            NovaLauncherRecentlyPlayedPodcast(
                id = "id-3",
                title = "title-3",
                initialReleaseTimestamp = 12,
                latestReleaseTimestamp = 12,
                lastUsedTimestamp = interactionDate3.timeSecs(),
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

        val podcasts = podcastDao.getNovaLauncherRecentlyPlayedPodcasts()

        val expected = listOf(
            NovaLauncherRecentlyPlayedPodcast(
                id = "id-1",
                title = "title-1",
                initialReleaseTimestamp = 0,
                latestReleaseTimestamp = 0,
                lastUsedTimestamp = interactionDate.timeSecs(),
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

        val podcasts = podcastDao.getNovaLauncherRecentlyPlayedPodcasts()

        val expected = listOf(
            NovaLauncherRecentlyPlayedPodcast(
                id = "id-2",
                title = "title-2",
                initialReleaseTimestamp = 0,
                latestReleaseTimestamp = 0,
                lastUsedTimestamp = interactionDate2.timeSecs(),
            ),
            NovaLauncherRecentlyPlayedPodcast(
                id = "id-3",
                title = "title-3",
                initialReleaseTimestamp = 0,
                latestReleaseTimestamp = 0,
                lastUsedTimestamp = interactionDate3.timeSecs(),
            ),
            NovaLauncherRecentlyPlayedPodcast(
                id = "id-1",
                title = "title-1",
                initialReleaseTimestamp = 0,
                latestReleaseTimestamp = 0,
                lastUsedTimestamp = interactionDate1.timeSecs(),
            ),
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun limitNovaLauncherRecentlyPlayedPodcastsTo200Podcasts() = runTest {
        List(250) {
            podcastDao.insert(Podcast(uuid = "id-$it"))
            episodeDao.insert(PodcastEpisode(uuid = "id-$it", podcastUuid = "id-$it", publishedDate = Date(), lastPlaybackInteraction = Date().time))
        }

        val inProgressEpisodes = podcastDao.getNovaLauncherRecentlyPlayedPodcasts()

        assertEquals(200, inProgressEpisodes.size)
    }

    @Test
    fun ignoreRecentlyPlayedPodcastsForNovaLauncherThatAreAtLeast60DaysOld() = runTest {
        val interactionDate1 = Date.from(Instant.now().minus(61, ChronoUnit.DAYS))
        val interactionDate2 = Date.from(Instant.now().minus(59, ChronoUnit.DAYS))
        podcastDao.insert(Podcast(uuid = "id-1", title = "title-1", isSubscribed = true))
        episodeDao.insert(PodcastEpisode(uuid = "id-1", publishedDate = Date(0), podcastUuid = "id-1", lastPlaybackInteraction = interactionDate1.time))

        podcastDao.insert(Podcast(uuid = "id-2", title = "title-2", isSubscribed = false))
        episodeDao.insert(PodcastEpisode(uuid = "id-2", publishedDate = Date(0), podcastUuid = "id-2", lastPlaybackInteraction = interactionDate2.time))

        val podcasts = podcastDao.getNovaLauncherRecentlyPlayedPodcasts()

        val expected = listOf(
            NovaLauncherRecentlyPlayedPodcast(
                id = "id-2",
                title = "title-2",
                initialReleaseTimestamp = 0,
                latestReleaseTimestamp = 0,
                lastUsedTimestamp = interactionDate2.timeSecs(),
            ),
        )
        assertEquals(expected, podcasts)
    }
}
