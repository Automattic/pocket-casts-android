package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.ExternalDataDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.UpNextDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.UserEpisodeDao
import au.com.shiftyjelly.pocketcasts.models.entity.CuratedPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcastList
import au.com.shiftyjelly.pocketcasts.models.entity.ExternalPodcastView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UpNextEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ExternalDataDaoTest {
    private lateinit var podcastDao: PodcastDao
    private lateinit var podcastEpisodeDao: EpisodeDao
    private lateinit var userEpisodeDao: UserEpisodeDao
    private lateinit var upNextDao: UpNextDao
    private lateinit var testDb: AppDatabase

    private lateinit var externalDataDao: ExternalDataDao

    @Before
    fun setupDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        podcastDao = testDb.podcastDao()
        podcastEpisodeDao = testDb.episodeDao()
        userEpisodeDao = testDb.userEpisodeDao()
        upNextDao = testDb.upNextDao()
        externalDataDao = testDb.externalDataDao()
    }

    @After
    fun closeDb() {
        testDb.close()
    }

    @Test
    fun useCorrectReleaseTimestampsForSubscribedPodcasts() = runTest {
        podcastDao.insert(Podcast(uuid = "id-1", title = "title-1", isSubscribed = true, podcastDescription = "description"))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-1", publishedDate = Date(0), podcastUuid = "id-1", lastPlaybackInteraction = 10))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-2", publishedDate = Date(2), podcastUuid = "id-1", lastPlaybackInteraction = null))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-3", publishedDate = Date(1), podcastUuid = "id-1", lastPlaybackInteraction = 13))

        podcastDao.insert(Podcast(uuid = "id-2", title = "title-2", isSubscribed = true))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-4", publishedDate = Date(5), podcastUuid = "id-2", lastPlaybackInteraction = 0))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-5", publishedDate = Date(4), podcastUuid = "id-2", lastPlaybackInteraction = 20))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-6", publishedDate = Date(3), podcastUuid = "id-2", lastPlaybackInteraction = 25))

        podcastDao.insert(Podcast(uuid = "id-3", title = "title-3", isSubscribed = true))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-7", publishedDate = Date(12), podcastUuid = "id-3", lastPlaybackInteraction = 0))

        podcastDao.insert(Podcast(uuid = "id-4", title = "title-4", isSubscribed = true))

        val podcasts = externalDataDao.getSubscribedPodcasts(PodcastsSortType.NAME_A_TO_Z, limit = 100)

        val expected = listOf(
            ExternalPodcast(
                id = "id-1",
                title = "title-1",
                description = "description",
                episodeCount = 3,
                _categories = "",
                initialReleaseTimestampMs = 0,
                latestReleaseTimestampMs = 2,
                lastUsedTimestampMs = 13,
            ),
            ExternalPodcast(
                id = "id-2",
                title = "title-2",
                description = "",
                episodeCount = 3,
                _categories = "",
                initialReleaseTimestampMs = 3,
                latestReleaseTimestampMs = 5,
                lastUsedTimestampMs = 25,
            ),
            ExternalPodcast(
                id = "id-3",
                title = "title-3",
                episodeCount = 1,
                description = "",
                _categories = "",
                initialReleaseTimestampMs = 12,
                latestReleaseTimestampMs = 12,
                lastUsedTimestampMs = 0,
            ),
            ExternalPodcast(
                id = "id-4",
                title = "title-4",
                description = "",
                episodeCount = 0,
                _categories = "",
                initialReleaseTimestampMs = null,
                latestReleaseTimestampMs = null,
                lastUsedTimestampMs = null,
            ),
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun includeOnlySubscribedPodcastsForSubscribedPodcasts() = runTest {
        podcastDao.insert(Podcast(uuid = "id-1", title = "title-1", isSubscribed = true))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-1", publishedDate = Date(0), podcastUuid = "id-1"))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-2", publishedDate = Date(0), podcastUuid = "id-1"))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-3", publishedDate = Date(0), podcastUuid = "id-1"))

        podcastDao.insert(Podcast(uuid = "id-2", title = "title-2", isSubscribed = false))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-4", publishedDate = Date(0), podcastUuid = "id-2"))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-5", publishedDate = Date(0), podcastUuid = "id-2"))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-6", publishedDate = Date(0), podcastUuid = "id-2"))

        val podcasts = externalDataDao.getSubscribedPodcasts(PodcastsSortType.NAME_A_TO_Z, limit = 100)

        val expected = listOf(
            ExternalPodcast(
                id = "id-1",
                title = "title-1",
                description = "",
                episodeCount = 3,
                _categories = "",
                initialReleaseTimestampMs = 0,
                latestReleaseTimestampMs = 0,
                lastUsedTimestampMs = null,
            ),
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun includeCategoriesForSubscribedPodcasts() = runTest {
        podcastDao.insert(Podcast(uuid = "id-1", title = "title-1", isSubscribed = true, podcastCategory = "category-1"))

        val podcasts = externalDataDao.getSubscribedPodcasts(PodcastsSortType.NAME_A_TO_Z, limit = 100)

        val expected = listOf(
            ExternalPodcast(
                id = "id-1",
                title = "title-1",
                description = "",
                episodeCount = 0,
                _categories = "category-1",
                initialReleaseTimestampMs = null,
                latestReleaseTimestampMs = null,
                lastUsedTimestampMs = null,
            ),
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun sortSubscribedPodcastsByAddedDate() = runTest {
        podcastDao.insert(Podcast(uuid = "id-1", title = "title-1", isSubscribed = true, addedDate = Date(3)))
        podcastDao.insert(Podcast(uuid = "id-2", title = "title-2", isSubscribed = true, addedDate = Date(4)))
        podcastDao.insert(Podcast(uuid = "id-3", title = "title-3", isSubscribed = true, addedDate = Date(2)))
        podcastDao.insert(Podcast(uuid = "id-4", title = "title-4", isSubscribed = true, addedDate = Date(1)))
        podcastDao.insert(Podcast(uuid = "id-5", title = "title-5", isSubscribed = true, addedDate = null))

        val podcastIds = externalDataDao.getSubscribedPodcasts(PodcastsSortType.DATE_ADDED_OLDEST_TO_NEWEST, limit = 100).map { it.id }

        assertEquals(listOf("id-4", "id-3", "id-1", "id-2", "id-5"), podcastIds)
    }

    @Test
    fun sortSubscribedPodcastsByTitle() = runTest {
        podcastDao.insert(Podcast(uuid = "id-1", title = "title-4", isSubscribed = true))
        podcastDao.insert(Podcast(uuid = "id-2", title = "title-3", isSubscribed = true))
        podcastDao.insert(Podcast(uuid = "id-3", title = "title-1", isSubscribed = true))
        podcastDao.insert(Podcast(uuid = "id-4", title = "title-2", isSubscribed = true))

        val podcastIds = externalDataDao.getSubscribedPodcasts(PodcastsSortType.NAME_A_TO_Z, limit = 100).map { it.id }

        assertEquals(listOf("id-3", "id-4", "id-2", "id-1"), podcastIds)
    }

    @Test
    fun sortingSubscribedPodcastsByTitleOmitsEnglishArticles() = runTest {
        podcastDao.insert(Podcast(uuid = "id-1", title = "The 1", isSubscribed = true))
        podcastDao.insert(Podcast(uuid = "id-2", title = "An 2", isSubscribed = true))
        podcastDao.insert(Podcast(uuid = "id-3", title = "A 3", isSubscribed = true))
        podcastDao.insert(Podcast(uuid = "id-4", title = "4", isSubscribed = true))
        podcastDao.insert(Podcast(uuid = "id-5", title = "the 5", isSubscribed = true))
        podcastDao.insert(Podcast(uuid = "id-6", title = "an 6", isSubscribed = true))
        podcastDao.insert(Podcast(uuid = "id-7", title = "a 7", isSubscribed = true))
        podcastDao.insert(Podcast(uuid = "id-8", title = "8", isSubscribed = true))

        val podcastIds = externalDataDao.getSubscribedPodcasts(PodcastsSortType.NAME_A_TO_Z, limit = 100).map { it.id }

        assertEquals(listOf("id-1", "id-2", "id-3", "id-4", "id-5", "id-6", "id-7", "id-8"), podcastIds)
    }

    @Test
    fun sortSubscribedPodcastsBylatestReleaseTimestamp() = runTest {
        podcastDao.insert(Podcast(uuid = "id-1", title = "title-1", isSubscribed = true))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-1", publishedDate = Date(0), podcastUuid = "id-1"))

        podcastDao.insert(Podcast(uuid = "id-2", title = "title-2", isSubscribed = true))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-2", publishedDate = Date(100), podcastUuid = "id-2"))

        podcastDao.insert(Podcast(uuid = "id-3", title = "title-3", isSubscribed = true))

        podcastDao.insert(Podcast(uuid = "id-4", title = "title-4", isSubscribed = true))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-3", publishedDate = Date(200), podcastUuid = "id-4"))

        val podcastIds = externalDataDao.getSubscribedPodcasts(PodcastsSortType.EPISODE_DATE_NEWEST_TO_OLDEST, limit = 100).map { it.id }

        assertEquals(listOf("id-4", "id-2", "id-1", "id-3"), podcastIds)
    }

    @Test
    fun sortSubscribedPodcastsByCustomSortOrder() = runTest {
        podcastDao.insert(Podcast(uuid = "id-1", title = "title-1", isSubscribed = true, sortPosition = 3))
        podcastDao.insert(Podcast(uuid = "id-2", title = "title-2", isSubscribed = true, sortPosition = 1))
        podcastDao.insert(Podcast(uuid = "id-3", title = "title-3", isSubscribed = true, sortPosition = 2))
        podcastDao.insert(Podcast(uuid = "id-4", title = "title-4", isSubscribed = true, sortPosition = 4))

        val podcastIds = externalDataDao.getSubscribedPodcasts(PodcastsSortType.DRAG_DROP, limit = 100).map { it.id }

        assertEquals(listOf("id-2", "id-3", "id-1", "id-4"), podcastIds)
    }

    @Test
    fun limitSubscribedPodcasts() = runTest {
        List(250) {
            podcastDao.insert(Podcast(uuid = "id-$it", isSubscribed = true))
        }

        val podcasts = externalDataDao.getSubscribedPodcasts(PodcastsSortType.NAME_A_TO_Z, limit = 60)

        assertEquals(60, podcasts.size)
    }

    @Test
    fun getAllTrendingCuratedPodcasts() = runTest {
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

        val podcasts = externalDataDao.getCuratedPodcastGroups(limitPerGroup = 100).trendingGroup()

        val expected = ExternalPodcastList(
            id = "trending",
            title = "Trending",
            podcasts = List(65) { ExternalPodcastView("id-$it", "title-$it", description = null) },
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun limitTrendingCuratedPodcasts() = runTest {
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

        val size = externalDataDao.getCuratedPodcastGroups(limitPerGroup = 5).trendingGroup()?.podcasts?.size

        assertEquals(5, size)
    }

    @Test
    fun doNotIncludeTrendingPodcastsThatAreSubscribedTo() = runTest {
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

        val podcasts = externalDataDao.getCuratedPodcastGroups(limitPerGroup = 100).trendingGroup()?.podcasts

        val expected = listOf(
            ExternalPodcastView("id-1", "title-1", description = null),
            ExternalPodcastView("id-2", "title-2", description = null),
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun getAllFeaturedCuratedPodcasts() = runTest {
        val curatedPodcasts = List(65) {
            CuratedPodcast(
                listId = "featured",
                listTitle = "Featured",
                podcastId = "id-$it",
                podcastTitle = "title-$it",
                podcastDescription = null,
            )
        }
        podcastDao.replaceAllCuratedPodcasts(curatedPodcasts)

        val podcasts = externalDataDao.getCuratedPodcastGroups(limitPerGroup = 100).featuruedGroup()

        val expected = ExternalPodcastList(
            id = "featured",
            title = "Featured",
            podcasts = List(65) { ExternalPodcastView("id-$it", "title-$it", description = null) },
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun limitFeaturedCuratedPodcasts() = runTest {
        val trendingPodcasts = List(65) {
            CuratedPodcast(
                listId = "featured",
                listTitle = "Featured",
                podcastId = "id-$it",
                podcastTitle = "title-$it",
                podcastDescription = null,
            )
        }
        podcastDao.replaceAllCuratedPodcasts(trendingPodcasts)

        val size = externalDataDao.getCuratedPodcastGroups(limitPerGroup = 5).featuruedGroup()?.podcasts?.size

        assertEquals(5, size)
    }

    @Test
    fun includeFeaturedPodcastsThatAreSubscribedTo() = runTest {
        val trendingPodcasts = List(3) {
            CuratedPodcast(
                listId = "featured",
                listTitle = "Featured",
                podcastId = "id-$it",
                podcastTitle = "title-$it",
                podcastDescription = null,
            )
        }
        podcastDao.replaceAllCuratedPodcasts(trendingPodcasts)
        podcastDao.insert(Podcast("id-0", isSubscribed = true))
        podcastDao.insert(Podcast("id-1", isSubscribed = false))

        val podcasts = externalDataDao.getCuratedPodcastGroups(limitPerGroup = 100).featuruedGroup()?.podcasts

        val expected = listOf(
            ExternalPodcastView("id-0", "title-0", description = null),
            ExternalPodcastView("id-1", "title-1", description = null),
            ExternalPodcastView("id-2", "title-2", description = null),
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun getAllGenericCuratedPodcasts() = runTest {
        val curatedPodcasts = List(10) {
            CuratedPodcast(
                listId = "bork-${it % 2}",
                listTitle = "Bork",
                podcastId = "id-$it",
                podcastTitle = "title-$it",
                podcastDescription = null,
            )
        }
        podcastDao.replaceAllCuratedPodcasts(curatedPodcasts)

        val groups = externalDataDao.getCuratedPodcastGroups(limitPerGroup = 100).genericGroups()

        val expected = mapOf(
            "bork-0" to ExternalPodcastList(
                id = "bork-0",
                title = "Bork",
                podcasts = List(5) { ExternalPodcastView("id-${it * 2}", "title-${it * 2}", description = null) },
            ),
            "bork-1" to ExternalPodcastList(
                id = "bork-1",
                title = "Bork",
                podcasts = List(5) { ExternalPodcastView("id-${it * 2 + 1}", "title-${it * 2 + 1}", description = null) },
            ),
        )
        assertEquals(expected, groups)
    }

    @Test
    fun limitGenericCuratedPodcasts() = runTest {
        val trendingPodcasts = List(10) {
            CuratedPodcast(
                listId = "bork",
                listTitle = "Bork",
                podcastId = "id-$it",
                podcastTitle = "title-$it",
                podcastDescription = null,
            )
        }
        podcastDao.replaceAllCuratedPodcasts(trendingPodcasts)

        val size = externalDataDao.getCuratedPodcastGroups(limitPerGroup = 5).genericGroups()["bork"]?.podcasts?.size

        assertEquals(5, size)
    }

    @Test
    fun doNotIncludeGenericCuratedPodcastsThatAreSubscribedTo() = runTest {
        val trendingPodcasts = List(3) {
            CuratedPodcast(
                listId = "bork",
                listTitle = "Bork",
                podcastId = "id-$it",
                podcastTitle = "title-$it",
                podcastDescription = null,
            )
        }
        podcastDao.replaceAllCuratedPodcasts(trendingPodcasts)
        podcastDao.insert(Podcast("id-0", isSubscribed = true))
        podcastDao.insert(Podcast("id-1", isSubscribed = false))

        val podcasts = externalDataDao.getCuratedPodcastGroups(limitPerGroup = 100).genericGroups()["bork"]?.podcasts

        val expected = listOf(
            ExternalPodcastView("id-1", "title-1", description = null),
            ExternalPodcastView("id-2", "title-2", description = null),
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun getOnlyTrendingPodcasts() = runTest {
        val curatedPodcasts = listOf(
            CuratedPodcast(
                listId = "trending",
                listTitle = "Featured",
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

        val podcasts = externalDataDao.getCuratedPodcastGroups(limitPerGroup = 100).trendingGroup()?.podcasts

        val expected = listOf(
            ExternalPodcastView("id-0", "title-0", description = null),
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun getOnlyFeaturedPodcasts() = runTest {
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

        val podcasts = externalDataDao.getCuratedPodcastGroups(limitPerGroup = 100).featuruedGroup()?.podcasts

        val expected = listOf(
            ExternalPodcastView("id-1", "title-1", description = null),
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun getOnlyGenericCuratedPodcasts() = runTest {
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
                listId = "bork",
                listTitle = "Bork",
                podcastId = "id-2",
                podcastTitle = "title-2",
                podcastDescription = null,
            ),
        )
        podcastDao.replaceAllCuratedPodcasts(curatedPodcasts)

        val groups = externalDataDao.getCuratedPodcastGroups(limitPerGroup = 100).genericGroups()

        val expected = mapOf(
            "bork" to ExternalPodcastList(
                id = "bork",
                title = "Bork",
                podcasts = listOf(ExternalPodcastView("id-2", "title-2", description = null)),
            ),
        )
        assertEquals(expected, groups)
    }

    @Test
    fun useCorrectReleaseTimestampsForRecentlyPlayedPodcasts() = runTest {
        val interactionDate1 = Date.from(Instant.now().minus(1, ChronoUnit.DAYS))
        val interactionDate2 = Date.from(Instant.now().minus(2, ChronoUnit.DAYS))
        val interactionDate3 = Date.from(Instant.now().minus(3, ChronoUnit.DAYS))
        podcastDao.insert(Podcast(uuid = "id-1", title = "title-1", isSubscribed = true))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-1", publishedDate = Date(0), podcastUuid = "id-1", lastPlaybackInteraction = interactionDate1.time))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-2", publishedDate = Date(2), podcastUuid = "id-1", lastPlaybackInteraction = null))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-3", publishedDate = Date(1), podcastUuid = "id-1", lastPlaybackInteraction = 13000))

        podcastDao.insert(Podcast(uuid = "id-2", title = "title-2", isSubscribed = false))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-4", publishedDate = Date(5), podcastUuid = "id-2", lastPlaybackInteraction = 0))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-5", publishedDate = Date(4), podcastUuid = "id-2", lastPlaybackInteraction = 20))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-6", publishedDate = Date(3), podcastUuid = "id-2", lastPlaybackInteraction = interactionDate2.time))

        podcastDao.insert(Podcast(uuid = "id-3", title = "title-3", isSubscribed = true))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-7", publishedDate = Date(12), podcastUuid = "id-3", lastPlaybackInteraction = interactionDate3.time))

        val podcasts = externalDataDao.getRecentlyPlayedPodcasts(limit = 100)

        val expected = listOf(
            ExternalPodcast(
                id = "id-1",
                title = "title-1",
                description = "",
                episodeCount = 3,
                _categories = "",
                initialReleaseTimestampMs = 0,
                latestReleaseTimestampMs = 2,
                lastUsedTimestampMs = interactionDate1.time,
            ),
            ExternalPodcast(
                id = "id-2",
                title = "title-2",
                episodeCount = 3,
                description = "",
                _categories = "",
                initialReleaseTimestampMs = 3,
                latestReleaseTimestampMs = 5,
                lastUsedTimestampMs = interactionDate2.time,
            ),
            ExternalPodcast(
                id = "id-3",
                title = "title-3",
                episodeCount = 1,
                description = "",
                _categories = "",
                initialReleaseTimestampMs = 12,
                latestReleaseTimestampMs = 12,
                lastUsedTimestampMs = interactionDate3.time,
            ),
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun includeOnlyInteractedWithPodcastsForRecentlyPlayedPodcasts() = runTest {
        val interactionDate = Date()
        podcastDao.insert(Podcast(uuid = "id-1", title = "title-1"))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-1", publishedDate = Date(0), podcastUuid = "id-1", lastPlaybackInteraction = interactionDate.time))

        podcastDao.insert(Podcast(uuid = "id-2", title = "title-2"))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-2", publishedDate = Date(0), podcastUuid = "id-2", lastPlaybackInteraction = null))

        podcastDao.insert(Podcast(uuid = "id-3", title = "title-3"))

        val podcasts = externalDataDao.getRecentlyPlayedPodcasts(limit = 100)

        val expected = listOf(
            ExternalPodcast(
                id = "id-1",
                title = "title-1",
                description = "",
                episodeCount = 1,
                _categories = "",
                initialReleaseTimestampMs = 0,
                latestReleaseTimestampMs = 0,
                lastUsedTimestampMs = interactionDate.time,
            ),
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun getRecentlyPlayedPodcastsSortedByInteractionDate() = runTest {
        val interactionDate1 = Date.from(Instant.now().minus(3, ChronoUnit.DAYS))
        val interactionDate2 = Date.from(Instant.now().minus(1, ChronoUnit.DAYS))
        val interactionDate3 = Date.from(Instant.now().minus(2, ChronoUnit.DAYS))
        podcastDao.insert(Podcast(uuid = "id-1", title = "title-1"))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-1", publishedDate = Date(0), podcastUuid = "id-1", lastPlaybackInteraction = interactionDate1.time))

        podcastDao.insert(Podcast(uuid = "id-2", title = "title-2"))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-2", publishedDate = Date(0), podcastUuid = "id-2", lastPlaybackInteraction = interactionDate2.time))

        podcastDao.insert(Podcast(uuid = "id-3", title = "title-3"))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-3", publishedDate = Date(0), podcastUuid = "id-3", lastPlaybackInteraction = interactionDate3.time))

        val podcasts = externalDataDao.getRecentlyPlayedPodcasts(limit = 100)

        val expected = listOf(
            ExternalPodcast(
                id = "id-2",
                title = "title-2",
                description = "",
                episodeCount = 1,
                _categories = "",
                initialReleaseTimestampMs = 0,
                latestReleaseTimestampMs = 0,
                lastUsedTimestampMs = interactionDate2.time,
            ),
            ExternalPodcast(
                id = "id-3",
                title = "title-3",
                description = "",
                episodeCount = 1,
                _categories = "",
                initialReleaseTimestampMs = 0,
                latestReleaseTimestampMs = 0,
                lastUsedTimestampMs = interactionDate3.time,
            ),
            ExternalPodcast(
                id = "id-1",
                title = "title-1",
                description = "",
                episodeCount = 1,
                _categories = "",
                initialReleaseTimestampMs = 0,
                latestReleaseTimestampMs = 0,
                lastUsedTimestampMs = interactionDate1.time,
            ),
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun limitRecentlyPlayedPodcasts() = runTest {
        List(250) {
            podcastDao.insert(Podcast(uuid = "id-$it"))
            podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-$it", podcastUuid = "id-$it", publishedDate = Date(), lastPlaybackInteraction = Date().time))
        }

        val podcasts = externalDataDao.getRecentlyPlayedPodcasts(limit = 100)

        assertEquals(100, podcasts.size)
    }

    @Test
    fun ignoreRecentlyPlayedPodcastsThatAreAtLeast60DaysOld() = runTest {
        val interactionDate1 = Date.from(Instant.now().minus(61, ChronoUnit.DAYS))
        val interactionDate2 = Date.from(Instant.now().minus(59, ChronoUnit.DAYS))
        podcastDao.insert(Podcast(uuid = "id-1", title = "title-1", isSubscribed = true))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-1", publishedDate = Date(0), podcastUuid = "id-1", lastPlaybackInteraction = interactionDate1.time))

        podcastDao.insert(Podcast(uuid = "id-2", title = "title-2", isSubscribed = false))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-2", publishedDate = Date(0), podcastUuid = "id-2", lastPlaybackInteraction = interactionDate2.time))

        val podcasts = externalDataDao.getRecentlyPlayedPodcasts(limit = 100)

        val expected = listOf(
            ExternalPodcast(
                id = "id-2",
                title = "title-2",
                description = "",
                episodeCount = 1,
                _categories = "",
                initialReleaseTimestampMs = 0,
                latestReleaseTimestampMs = 0,
                lastUsedTimestampMs = interactionDate2.time,
            ),
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun includeCategoriesForRecentlyPlayedPodcasts() = runTest {
        val interactionDate1 = Date.from(Instant.now().minus(1, ChronoUnit.DAYS))
        podcastDao.insert(Podcast(uuid = "id-1", title = "title-1", podcastCategory = "category1"))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-1", publishedDate = Date(0), podcastUuid = "id-1", lastPlaybackInteraction = interactionDate1.time))

        val podcasts = externalDataDao.getRecentlyPlayedPodcasts(limit = 100)

        val expected = listOf(
            ExternalPodcast(
                id = "id-1",
                title = "title-1",
                description = "",
                episodeCount = 1,
                _categories = "category1",
                initialReleaseTimestampMs = 0,
                latestReleaseTimestampMs = 0,
                lastUsedTimestampMs = interactionDate1.time,
            ),
        )
        assertEquals(expected, podcasts)
    }

    @Test
    fun getNewEpisodeReleases() = runTest {
        val now = Instant.now()
        val publishedDate1 = Date.from(now)
        val publishedDate2 = Date.from(now.minus(10, ChronoUnit.MINUTES))
        val publishedDate3 = Date.from(now.minus(20, ChronoUnit.MINUTES))
        val episodes = listOf(
            PodcastEpisode(
                uuid = "id-1",
                podcastUuid = "p-id-1",
                title = "title-1",
                duration = 100.12,
                playedUpTo = 50.0,
                season = 0,
                number = 11,
                publishedDate = publishedDate1,
                lastPlaybackInteraction = 20,
            ),
            PodcastEpisode(
                uuid = "id-2",
                podcastUuid = "p-id-1",
                title = "title-2",
                duration = 4120.0,
                playedUpTo = 2021.24,
                season = 7,
                number = null,
                publishedDate = publishedDate2,
                lastPlaybackInteraction = null,
                fileType = "video/mp4",
            ),
            PodcastEpisode(
                uuid = "id-3",
                podcastUuid = "p-id-2",
                title = "title-3",
                duration = 2330.0,
                playedUpTo = 0.0,
                season = null,
                number = 399,
                publishedDate = publishedDate3,
                lastPlaybackInteraction = 0,
                episodeStatus = EpisodeStatusEnum.DOWNLOADED,
            ),
        )
        podcastEpisodeDao.insertAll(episodes)
        podcastDao.insert(Podcast(uuid = "p-id-1", title = "p-title-1", isSubscribed = true))
        podcastDao.insert(Podcast(uuid = "p-id-2", title = "p-title-2", isSubscribed = true))

        val newEpisodes = externalDataDao.getNewEpisodes(limit = 100)

        val expected = listOf(
            ExternalEpisode.Podcast(
                id = "id-1",
                podcastId = "p-id-1",
                title = "title-1",
                podcastTitle = "p-title-1",
                durationMs = 100_120,
                playbackPositionMs = 50_000,
                seasonNumber = 0,
                episodeNumber = 11,
                releaseTimestampMs = publishedDate1.time,
                lastUsedTimestampMs = 20,
                isVideo = false,
                isDownloaded = false,
            ),
            ExternalEpisode.Podcast(
                id = "id-2",
                podcastId = "p-id-1",
                podcastTitle = "p-title-1",
                title = "title-2",
                durationMs = 4_120_000,
                playbackPositionMs = 2_021_240,
                seasonNumber = 7,
                episodeNumber = null,
                releaseTimestampMs = publishedDate2.time,
                lastUsedTimestampMs = null,
                isVideo = true,
                isDownloaded = false,
            ),
            ExternalEpisode.Podcast(
                id = "id-3",
                podcastId = "p-id-2",
                podcastTitle = "p-title-2",
                title = "title-3",
                durationMs = 2_330_000,
                playbackPositionMs = 0,
                seasonNumber = null,
                episodeNumber = 399,
                releaseTimestampMs = publishedDate3.time,
                lastUsedTimestampMs = 0,
                isVideo = false,
                isDownloaded = true,
            ),
        )
        assertEquals(expected, newEpisodes)
    }

    @Test
    fun getNewEpisodeReleasesSortedByReleaseDate() = runTest {
        val now = Instant.now()
        val publishedDate1 = Date.from(now.minus(10, ChronoUnit.MINUTES))
        val publishedDate2 = Date.from(now.minus(20, ChronoUnit.MINUTES))
        val publishedDate3 = Date.from(now)
        val episodes = listOf(
            PodcastEpisode(
                uuid = "id-1",
                publishedDate = publishedDate1,
            ),
            PodcastEpisode(
                uuid = "id-2",
                publishedDate = publishedDate2,
            ),
            PodcastEpisode(
                uuid = "id-3",
                publishedDate = publishedDate3,
            ),
        )
        podcastEpisodeDao.insertAll(episodes)
        podcastDao.insert(Podcast(uuid = "", isSubscribed = true))

        val newEpisodes = externalDataDao.getNewEpisodes(limit = 100)

        val expected = listOf(
            ExternalEpisode.Podcast(
                id = "id-3",
                podcastId = "",
                title = "",
                podcastTitle = "",
                durationMs = 0,
                playbackPositionMs = 0,
                seasonNumber = null,
                episodeNumber = null,
                releaseTimestampMs = publishedDate3.time,
                lastUsedTimestampMs = null,
                isDownloaded = false,
                isVideo = false,
            ),
            ExternalEpisode.Podcast(
                id = "id-1",
                podcastId = "",
                title = "",
                podcastTitle = "",
                durationMs = 0,
                playbackPositionMs = 0,
                seasonNumber = null,
                episodeNumber = null,
                releaseTimestampMs = publishedDate1.time,
                lastUsedTimestampMs = null,
                isDownloaded = false,
                isVideo = false,
            ),
            ExternalEpisode.Podcast(
                id = "id-2",
                podcastId = "",
                title = "",
                podcastTitle = "",
                durationMs = 0,
                playbackPositionMs = 0,
                seasonNumber = null,
                episodeNumber = null,
                releaseTimestampMs = publishedDate2.time,
                lastUsedTimestampMs = null,
                isDownloaded = false,
                isVideo = false,
            ),
        )
        assertEquals(expected, newEpisodes)
    }

    @Test
    fun limitNewEpisodeReleases() = runTest {
        val episodes = List(550) {
            PodcastEpisode(
                uuid = "id-$it",
                publishedDate = Date(),
            )
        }
        podcastEpisodeDao.insertAll(episodes)
        podcastDao.insert(Podcast(uuid = "", isSubscribed = true))

        val newEpisodes = externalDataDao.getNewEpisodes(limit = 75)

        assertEquals(75, newEpisodes.size)
    }

    @Test
    fun ignoreNewEpisodeReleasesThatAreArchived() = runTest {
        val publishedDate = Date()
        val episodes = listOf(
            PodcastEpisode(
                uuid = "id-1",
                isArchived = false,
                publishedDate = publishedDate,
            ),
            PodcastEpisode(
                uuid = "id-2",
                isArchived = true,
                publishedDate = publishedDate,
            ),
        )
        podcastEpisodeDao.insertAll(episodes)
        podcastDao.insert(Podcast(uuid = "", isSubscribed = true))

        val newEpisodes = externalDataDao.getNewEpisodes(limit = 100)

        val expected = listOf(
            ExternalEpisode.Podcast(
                id = "id-1",
                podcastId = "",
                title = "",
                podcastTitle = "",
                durationMs = 0,
                playbackPositionMs = 0,
                seasonNumber = null,
                episodeNumber = null,
                releaseTimestampMs = publishedDate.time,
                lastUsedTimestampMs = null,
                isDownloaded = false,
                isVideo = false,
            ),
        )
        assertEquals(expected, newEpisodes)
    }

    @Test
    fun ignoreNewEpisodeReleasesThatArePlayed() = runTest {
        val publishedDate = Date()
        val episodes = listOf(
            PodcastEpisode(
                uuid = "id-1",
                playingStatus = EpisodePlayingStatus.NOT_PLAYED,
                publishedDate = publishedDate,
            ),
            PodcastEpisode(
                uuid = "id-2",
                playingStatus = EpisodePlayingStatus.IN_PROGRESS,
                publishedDate = publishedDate,
            ),
            PodcastEpisode(
                uuid = "id-3",
                playingStatus = EpisodePlayingStatus.COMPLETED,
                publishedDate = publishedDate,
            ),
        )
        podcastEpisodeDao.insertAll(episodes)
        podcastDao.insert(Podcast(uuid = "", isSubscribed = true))

        val newEpisodes = externalDataDao.getNewEpisodes(limit = 100)

        val expected = listOf(
            ExternalEpisode.Podcast(
                id = "id-1",
                podcastId = "",
                title = "",
                podcastTitle = "",
                durationMs = 0,
                playbackPositionMs = 0,
                seasonNumber = null,
                episodeNumber = null,
                releaseTimestampMs = publishedDate.time,
                lastUsedTimestampMs = null,
                isDownloaded = false,
                isVideo = false,
            ),
        )
        assertEquals(expected, newEpisodes)
    }

    @Test
    fun ignoreNewEpisodeReleasesThatAreAtLeastTwoWeeksOld() = runTest {
        val publishedDate1 = Date.from(Instant.now().minus(15, ChronoUnit.DAYS))
        val publishedDate2 = Date.from(Instant.now().minus(13, ChronoUnit.DAYS))
        val episodes = listOf(
            PodcastEpisode(
                uuid = "id-1",
                publishedDate = publishedDate1,
            ),
            PodcastEpisode(
                uuid = "id-2",
                publishedDate = publishedDate2,
            ),
        )
        podcastEpisodeDao.insertAll(episodes)
        podcastDao.insert(Podcast(uuid = "", isSubscribed = true))

        val newEpisodes = externalDataDao.getNewEpisodes(limit = 100)

        val expected = listOf(
            ExternalEpisode.Podcast(
                id = "id-2",
                podcastId = "",
                title = "",
                podcastTitle = "",
                durationMs = 0,
                playbackPositionMs = 0,
                seasonNumber = null,
                episodeNumber = null,
                releaseTimestampMs = publishedDate2.time,
                lastUsedTimestampMs = null,
                isDownloaded = false,
                isVideo = false,
            ),
        )
        assertEquals(expected, newEpisodes)
    }

    @Test
    fun ignoreNewEpisodeReleasesForUnsubscribedPodcasts() = runTest {
        val publishedDate = Date()
        val episodes = listOf(
            PodcastEpisode(
                uuid = "id-1",
                podcastUuid = "p-id-1",
                publishedDate = publishedDate,
            ),
            PodcastEpisode(
                uuid = "id-2",
                podcastUuid = "p-id-2",
                publishedDate = publishedDate,
            ),
        )
        podcastEpisodeDao.insertAll(episodes)
        podcastDao.insert(Podcast(uuid = "p-id-1", isSubscribed = true))
        podcastDao.insert(Podcast(uuid = "p-id-2", isSubscribed = false))

        val newEpisodes = externalDataDao.getNewEpisodes(limit = 100)

        val expected = listOf(
            ExternalEpisode.Podcast(
                id = "id-1",
                podcastId = "p-id-1",
                podcastTitle = "",
                title = "",
                durationMs = 0,
                playbackPositionMs = 0,
                seasonNumber = null,
                episodeNumber = null,
                releaseTimestampMs = publishedDate.time,
                lastUsedTimestampMs = null,
                isDownloaded = false,
                isVideo = false,
            ),
        )
        assertEquals(expected, newEpisodes)
    }

    @Test
    fun getInProgressEpisodes() = runTest {
        val episodes = listOf(
            PodcastEpisode(
                uuid = "id-1",
                podcastUuid = "p-id-1",
                title = "title-1",
                duration = 100.12,
                playedUpTo = 50.0,
                season = 0,
                number = 11,
                playingStatus = EpisodePlayingStatus.IN_PROGRESS,
                publishedDate = Date(0),
                lastPlaybackInteraction = 74,
            ),
            PodcastEpisode(
                uuid = "id-2",
                podcastUuid = "p-id-1",
                title = "title-2",
                duration = 4120.0,
                playedUpTo = 2021.24,
                season = 7,
                number = null,
                playingStatus = EpisodePlayingStatus.IN_PROGRESS,
                publishedDate = Date(2),
                lastPlaybackInteraction = 0,
                episodeStatus = EpisodeStatusEnum.DOWNLOADED,
            ),
            PodcastEpisode(
                uuid = "id-3",
                podcastUuid = "p-id-2",
                title = "title-3",
                duration = 2330.0,
                playedUpTo = 0.0,
                season = null,
                number = 399,
                playingStatus = EpisodePlayingStatus.IN_PROGRESS,
                publishedDate = Date(3),
                lastPlaybackInteraction = null,
                fileType = "video/",
            ),
        )
        podcastEpisodeDao.insertAll(episodes)
        podcastDao.insert(Podcast(uuid = "p-id-1", title = "p-title-1"))
        podcastDao.insert(Podcast(uuid = "p-id-2", title = "p-title-2"))

        val inProgressEpisodes = externalDataDao.getInProgressEpisodes(limit = 100, currentTime = 0)

        val expected = listOf(
            ExternalEpisode.Podcast(
                id = "id-1",
                podcastId = "p-id-1",
                title = "title-1",
                podcastTitle = "p-title-1",
                durationMs = 100_120,
                playbackPositionMs = 50_000,
                seasonNumber = 0,
                episodeNumber = 11,
                releaseTimestampMs = 0,
                lastUsedTimestampMs = 74,
                isDownloaded = false,
                isVideo = false,
            ),
            ExternalEpisode.Podcast(
                id = "id-2",
                podcastId = "p-id-1",
                title = "title-2",
                podcastTitle = "p-title-1",
                durationMs = 4_120_000,
                playbackPositionMs = 2_021_240,
                seasonNumber = 7,
                episodeNumber = null,
                releaseTimestampMs = 2,
                lastUsedTimestampMs = 0,
                isDownloaded = true,
                isVideo = false,
            ),
            ExternalEpisode.Podcast(
                id = "id-3",
                podcastId = "p-id-2",
                title = "title-3",
                podcastTitle = "p-title-2",
                durationMs = 2_330_000,
                playbackPositionMs = 0,
                seasonNumber = null,
                episodeNumber = 399,
                releaseTimestampMs = 3,
                lastUsedTimestampMs = null,
                isDownloaded = false,
                isVideo = true,
            ),
        )
        assertEquals(expected, inProgressEpisodes)
    }

    @Test
    fun getInProgressEpisodesrSortedByInteractionDate() = runTest {
        val episodes = listOf(
            PodcastEpisode(
                uuid = "id-1",
                playingStatus = EpisodePlayingStatus.IN_PROGRESS,
                publishedDate = Date(2),
                lastPlaybackInteraction = 1,
            ),
            PodcastEpisode(
                uuid = "id-2",
                playingStatus = EpisodePlayingStatus.IN_PROGRESS,
                publishedDate = Date(3),
                lastPlaybackInteraction = null,
            ),
            PodcastEpisode(
                uuid = "id-3",
                playingStatus = EpisodePlayingStatus.IN_PROGRESS,
                publishedDate = Date(1),
                lastPlaybackInteraction = 3,
            ),
        )
        podcastEpisodeDao.insertAll(episodes)
        podcastDao.insert(Podcast())

        val inProgressEpisodes = externalDataDao.getInProgressEpisodes(limit = 100, currentTime = 0)

        val expected = listOf(
            ExternalEpisode.Podcast(
                id = "id-3",
                podcastId = "",
                title = "",
                podcastTitle = "",
                durationMs = 0,
                playbackPositionMs = 0,
                seasonNumber = null,
                episodeNumber = null,
                releaseTimestampMs = 1,
                lastUsedTimestampMs = 3,
                isDownloaded = false,
                isVideo = false,
            ),
            ExternalEpisode.Podcast(
                id = "id-1",
                podcastId = "",
                title = "",
                podcastTitle = "",
                durationMs = 0,
                playbackPositionMs = 0,
                seasonNumber = null,
                episodeNumber = null,
                releaseTimestampMs = 2,
                lastUsedTimestampMs = 1,
                isDownloaded = false,
                isVideo = false,
            ),
            ExternalEpisode.Podcast(
                id = "id-2",
                podcastId = "",
                title = "",
                podcastTitle = "",
                durationMs = 0,
                playbackPositionMs = 0,
                seasonNumber = null,
                episodeNumber = null,
                releaseTimestampMs = 3,
                lastUsedTimestampMs = null,
                isDownloaded = false,
                isVideo = false,
            ),
        )
        assertEquals(expected, inProgressEpisodes)
    }

    @Test
    fun limitInProgressEpisodes() = runTest {
        val episodes = List(550) {
            PodcastEpisode(
                uuid = "id-$it",
                publishedDate = Date(0),
                playingStatus = EpisodePlayingStatus.IN_PROGRESS,
            )
        }
        podcastEpisodeDao.insertAll(episodes)
        podcastDao.insert(Podcast())

        val inProgressEpisodes = externalDataDao.getInProgressEpisodes(limit = 20, currentTime = 0)

        assertEquals(20, inProgressEpisodes.size)
    }

    @Test
    fun ignoreInProgressEpisodesThatAreArchived() = runTest {
        val episodes = listOf(
            PodcastEpisode(
                uuid = "id-1",
                isArchived = false,
                publishedDate = Date(0),
                playingStatus = EpisodePlayingStatus.IN_PROGRESS,
            ),
            PodcastEpisode(
                uuid = "id-2",
                isArchived = true,
                publishedDate = Date(0),
                playingStatus = EpisodePlayingStatus.IN_PROGRESS,
            ),
        )
        podcastEpisodeDao.insertAll(episodes)
        podcastDao.insert(Podcast())

        val inProgressEpisodes = externalDataDao.getInProgressEpisodes(limit = 100, currentTime = 0)

        val expected = listOf(
            ExternalEpisode.Podcast(
                id = "id-1",
                podcastId = "",
                title = "",
                podcastTitle = "",
                durationMs = 0,
                playbackPositionMs = 0,
                seasonNumber = null,
                episodeNumber = null,
                releaseTimestampMs = 0,
                lastUsedTimestampMs = null,
                isDownloaded = false,
                isVideo = false,
            ),
        )
        assertEquals(expected, inProgressEpisodes)
    }

    @Test
    fun ignoreInProgressEpisodesThatAreNotInProgress() = runTest {
        val episodes = listOf(
            PodcastEpisode(
                uuid = "id-1",
                publishedDate = Date(0),
                playingStatus = EpisodePlayingStatus.IN_PROGRESS,
            ),
            PodcastEpisode(
                uuid = "id-2",
                publishedDate = Date(0),
                playingStatus = EpisodePlayingStatus.NOT_PLAYED,
            ),
            PodcastEpisode(
                uuid = "id-3",
                publishedDate = Date(0),
                playingStatus = EpisodePlayingStatus.COMPLETED,
            ),
        )
        podcastEpisodeDao.insertAll(episodes)
        podcastDao.insert(Podcast())

        val inProgressEpisodes = externalDataDao.getInProgressEpisodes(limit = 100, currentTime = 0)

        val expected = listOf(
            ExternalEpisode.Podcast(
                id = "id-1",
                podcastId = "",
                title = "",
                podcastTitle = "",
                durationMs = 0,
                playbackPositionMs = 0,
                seasonNumber = null,
                episodeNumber = null,
                releaseTimestampMs = 0,
                lastUsedTimestampMs = null,
                isDownloaded = false,
                isVideo = false,
            ),
        )
        assertEquals(expected, inProgressEpisodes)
    }

    @Test
    fun getUpNextPodcastEpisodes() = runTest {
        val podcastEpisode = PodcastEpisode(
            uuid = "id-1",
            podcastUuid = "p-id-1",
            title = "title-1",
            duration = 1000.0,
            playedUpTo = 500.0,
            season = 10,
            number = 4,
            publishedDate = Date(800),
            lastPlaybackInteraction = 400,
            fileType = "video/*",
            episodeStatus = EpisodeStatusEnum.DOWNLOADED,
        )
        podcastEpisodeDao.insert(podcastEpisode)
        upNextDao.insert(UpNextEpisode(episodeUuid = "id-1"))
        podcastDao.insert(Podcast(uuid = "p-id-1", title = "p-title-1", isSubscribed = true))

        val episodes = externalDataDao.observeUpNextQueue(limit = 100).first()

        val expected = listOf(
            ExternalEpisode.Podcast(
                id = "id-1",
                title = "title-1",
                durationMs = 1_000_000,
                playbackPositionMs = 500_000,
                releaseTimestampMs = 800,
                podcastId = "p-id-1",
                podcastTitle = "p-title-1",
                seasonNumber = 10,
                episodeNumber = 4,
                lastUsedTimestampMs = 400,
                isDownloaded = true,
                isVideo = true,
            ),
        )
        assertEquals(expected, episodes)
    }

    @Test
    fun getUpNextUserEpisodes() = runTest {
        val userEpisode = UserEpisode(
            uuid = "id-1",
            title = "title-1",
            duration = 555.0,
            playedUpTo = 200.0,
            publishedDate = Date(100),
            artworkUrl = "artwork-url-1",
            tintColorIndex = 20,
            fileType = "video/*",
            episodeStatus = EpisodeStatusEnum.DOWNLOADED,
        )
        userEpisodeDao.insert(userEpisode)
        upNextDao.insert(UpNextEpisode(episodeUuid = "id-1"))

        val episodes = externalDataDao.observeUpNextQueue(limit = 100).first()

        val expected = listOf(
            ExternalEpisode.User(
                id = "id-1",
                title = "title-1",
                durationMs = 555_000,
                playbackPositionMs = 200_000,
                releaseTimestampMs = 100,
                artworkUrl = "artwork-url-1",
                tintColorIndex = 20,
                isDownloaded = true,
                isVideo = true,
            ),
        )
        assertEquals(expected, episodes)
    }

    @Test
    fun getUpNextEpisodesInCorrectOrder() = runTest {
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-1", publishedDate = Date()))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-2", publishedDate = Date()))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-3", publishedDate = Date()))
        userEpisodeDao.insert(UserEpisode(uuid = "id-4", publishedDate = Date()))
        userEpisodeDao.insert(UserEpisode(uuid = "id-5", publishedDate = Date()))
        podcastDao.insert(Podcast())
        upNextDao.insertAll(
            listOf(
                UpNextEpisode(episodeUuid = "id-1", position = 0),
                UpNextEpisode(episodeUuid = "id-2", position = 4),
                UpNextEpisode(episodeUuid = "id-3", position = 1),
                UpNextEpisode(episodeUuid = "id-4", position = 3),
                UpNextEpisode(episodeUuid = "id-5", position = 2),
            ),
        )

        val episodIds = externalDataDao.observeUpNextQueue(limit = 100).first().map(ExternalEpisode::id)

        assertEquals(listOf("id-1", "id-3", "id-5", "id-4", "id-2"), episodIds)
    }

    @Test
    fun ignoreUnknnownUpNextEpisodes() = runTest {
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-1", publishedDate = Date()))
        podcastEpisodeDao.insert(PodcastEpisode(uuid = "id-3", publishedDate = Date()))
        userEpisodeDao.insert(UserEpisode(uuid = "id-5", publishedDate = Date()))
        podcastDao.insert(Podcast())
        upNextDao.insertAll(
            listOf(
                UpNextEpisode(episodeUuid = "id-1", position = 0),
                UpNextEpisode(episodeUuid = "id-2", position = 1),
                UpNextEpisode(episodeUuid = "id-3", position = 2),
                UpNextEpisode(episodeUuid = "id-4", position = 3),
                UpNextEpisode(episodeUuid = "id-5", position = 5),
            ),
        )

        val episodIds = externalDataDao.observeUpNextQueue(limit = 100).first().map(ExternalEpisode::id)

        assertEquals(listOf("id-1", "id-3", "id-5"), episodIds)
    }

    @Test
    fun limitUpNextEpisodes() = runTest {
        val podcastEpisodes = List(30) { PodcastEpisode(uuid = "id-$it", publishedDate = Date()) }
        podcastEpisodeDao.insertAll(podcastEpisodes)
        podcastDao.insert(Podcast())
        upNextDao.insertAll(podcastEpisodes.mapIndexed { index, episode -> UpNextEpisode(episodeUuid = episode.uuid, position = index) })

        val episodes = externalDataDao.observeUpNextQueue(limit = 8).first()

        assertEquals(8, episodes.size)
    }
}
