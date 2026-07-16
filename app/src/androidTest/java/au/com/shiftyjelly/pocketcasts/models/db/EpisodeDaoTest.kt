package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeDownloadFailureStatistics
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.DailyListenedTime
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.utils.extensions.escapeLike
import com.squareup.moshi.Moshi
import java.time.Instant
import java.util.Date
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EpisodeDaoTest {
    lateinit var episodeDao: EpisodeDao
    lateinit var podcastDao: PodcastDao
    lateinit var testDb: AppDatabase

    @Before
    fun setupDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(ModelModule.provideRoomConverters(Moshi.Builder().build()))
            .build()
        episodeDao = testDb.episodeDao()
        podcastDao = testDb.podcastDao()
    }

    @After
    fun closeDb() {
        testDb.close()
    }

    @Test
    fun insertAndFindEpisode() = runBlocking {
        val episode = PodcastEpisode(uuid = "id", publishedDate = Date())

        episodeDao.insertBlocking(episode)

        assertEquals(episode, episodeDao.findByUuid(episode.uuid))
    }

    @Test
    fun insertAllAndFindEpisodes() = runBlocking {
        val episodes = listOf(
            PodcastEpisode(uuid = "1", publishedDate = Date()),
            PodcastEpisode(uuid = "2", publishedDate = Date()),
        )

        episodeDao.insertAllBlocking(episodes)

        assertEquals(episodes[0], episodeDao.findByUuid(episodes[0].uuid))
        assertEquals(episodes[1], episodeDao.findByUuid(episodes[1].uuid))
    }

    @Test
    fun getFailedDownloadStatisticsWithNoEpisodes() = runBlocking {
        val statistics = episodeDao.getFailedDownloadsStatistics()

        val expected = EpisodeDownloadFailureStatistics(
            count = 0,
            newestTimestamp = null,
            oldestTimestamp = null,
        )
        assertEquals(expected, statistics)
    }

    @Test
    fun getFailedDownloadStatisticsWithSingleEpisode() = runBlocking {
        val episode = PodcastEpisode(
            uuid = "1",
            publishedDate = Date(),
            downloadStatus = EpisodeDownloadStatus.DownloadFailed,
            lastDownloadAttemptDate = Date.from(Instant.EPOCH),
        )
        episodeDao.insertBlocking(episode)

        val statistics = episodeDao.getFailedDownloadsStatistics()

        val expected = EpisodeDownloadFailureStatistics(
            count = 1,
            newestTimestamp = Instant.EPOCH,
            oldestTimestamp = Instant.EPOCH,
        )
        assertEquals(expected, statistics)
    }

    @Test
    fun getFailedDownloadStatisticsWithMultipleEpisodes() = runBlocking {
        val episodes = List(10) { index ->
            PodcastEpisode(
                uuid = index.toString(),
                publishedDate = Date(),
                downloadStatus = EpisodeDownloadStatus.DownloadFailed,
                lastDownloadAttemptDate = Date.from(Instant.EPOCH.plusMillis(index.toLong())),
            )
        }
        episodeDao.insertAllBlocking(episodes)

        val statistics = episodeDao.getFailedDownloadsStatistics()

        val expected = EpisodeDownloadFailureStatistics(
            count = episodes.size.toLong(),
            newestTimestamp = Instant.EPOCH.plusMillis(episodes.size.toLong() - 1),
            oldestTimestamp = Instant.EPOCH,
        )
        assertEquals(expected, statistics)
    }

    @Test
    fun getFailedDownloadStatisticsWithMissingDownloadAttemptDate() = runBlocking {
        val episodes = List(10) { index ->
            PodcastEpisode(
                uuid = index.toString(),
                publishedDate = Date(),
                downloadStatus = EpisodeDownloadStatus.DownloadFailed,
                lastDownloadAttemptDate = null,
            )
        }
        episodeDao.insertAllBlocking(episodes)

        val statistics = episodeDao.getFailedDownloadsStatistics()

        val expected = EpisodeDownloadFailureStatistics(
            count = episodes.size.toLong(),
            newestTimestamp = null,
            oldestTimestamp = null,
        )
        assertEquals(expected, statistics)
    }

    @Test
    fun getFailedDownloadStatisticsOnlyForFailedDownloads() = runBlocking {
        val episodes = EpisodeDownloadStatus.entries.map { entry ->
            PodcastEpisode(
                uuid = entry.ordinal.toString(),
                publishedDate = Date(),
                downloadStatus = entry,
                lastDownloadAttemptDate = Date.from(Instant.EPOCH.plusMillis(entry.ordinal.toLong())),
            )
        }
        episodeDao.insertAllBlocking(episodes)

        val statistics = episodeDao.getFailedDownloadsStatistics()

        val expected = EpisodeDownloadFailureStatistics(
            count = 1,
            newestTimestamp = Instant.EPOCH.plusMillis(EpisodeDownloadStatus.DownloadFailed.ordinal.toLong()),
            oldestTimestamp = Instant.EPOCH.plusMillis(EpisodeDownloadStatus.DownloadFailed.ordinal.toLong()),
        )
        assertEquals(expected, statistics)
    }

    @Test
    fun getFilteredPlaybackHistoryResultForMatchedEpisodeTitle() = runTest {
        val query = "test"
        val episodes = listOf(PodcastEpisode(uuid = "1", title = "Test Episode", podcastUuid = "podcast_uuid", publishedDate = Date(), lastPlaybackInteraction = 1000))
        val podcast = Podcast(uuid = "podcast_uuid")
        episodeDao.insertAllBlocking(episodes)
        podcastDao.insertBlocking(podcast)

        val result = episodeDao.filteredPlaybackHistoryFlow(query.escapeLike('\\')).first()
        assertEquals(episodes, result)
    }

    @Test
    fun getFilteredPlaybackHistoryResultForMatchedPodcastTitle() = runTest {
        val query = "test"
        val episodes = listOf(PodcastEpisode(uuid = "1", title = "Episode", podcastUuid = "podcast_uuid", publishedDate = Date(), lastPlaybackInteraction = 1000))
        val podcast = Podcast(uuid = "podcast_uuid", title = "Test Podcast")
        episodeDao.insertAllBlocking(episodes)
        podcastDao.insertBlocking(podcast)

        val result = episodeDao.filteredPlaybackHistoryFlow(query.escapeLike('\\')).first()
        assertEquals(episodes, result)
    }

    @Test
    fun getOrderedFilteredPlaybackHistoryResults() = runTest {
        val query = "test"
        val episode1 = PodcastEpisode(uuid = "1", title = "Test Episode 1", publishedDate = Date(), lastPlaybackInteraction = 1000)
        val episode2 = PodcastEpisode(uuid = "2", title = "Test Episode 2", publishedDate = Date(), lastPlaybackInteraction = 2000)
        val episodes = listOf(episode2, episode1)
        episodeDao.insertAllBlocking(episodes)

        val result = episodeDao.filteredPlaybackHistoryFlow(query.escapeLike('\\')).first()
        assertEquals(episodes, result)
    }

    @Test
    fun getFilteredPlaybackHistoryResultForNoMatch() = runTest {
        val query = "test"
        val episodes = listOf(PodcastEpisode(uuid = "1", title = "Episode", podcastUuid = "podcast_uuid", publishedDate = Date(), lastPlaybackInteraction = 1000))
        episodeDao.insertAllBlocking(episodes)

        val result = episodeDao.filteredPlaybackHistoryFlow(query.escapeLike('\\')).first()
        assertEquals(emptyList<PodcastEpisode>(), result)
    }

    @Test
    fun dailyListenedTimeGroupsPlaybackByDay() = runTest {
        val dayOne = Instant.parse("2024-06-15T12:00:00Z").toEpochMilli()
        val dayTwo = Instant.parse("2024-06-17T12:00:00Z").toEpochMilli()
        val beforeRange = Instant.parse("2024-06-10T12:00:00Z").toEpochMilli()
        val episodes = listOf(
            PodcastEpisode(uuid = "1", publishedDate = Date(), lastPlaybackInteraction = dayOne, playedUpTo = 100.0),
            PodcastEpisode(uuid = "2", publishedDate = Date(), lastPlaybackInteraction = dayOne, playedUpTo = 50.0),
            PodcastEpisode(uuid = "3", publishedDate = Date(), lastPlaybackInteraction = dayTwo, playedUpTo = 30.0),
            PodcastEpisode(uuid = "4", publishedDate = Date(), lastPlaybackInteraction = beforeRange, playedUpTo = 999.0),
        )
        episodeDao.insertAllBlocking(episodes)

        val result = episodeDao.dailyListenedTime(fromEpochMs = Instant.parse("2024-06-14T00:00:00Z").toEpochMilli())

        assertEquals(2, result.size)
        assertEquals(150.0, result[0].totalPlayedSeconds, 0.001)
        assertEquals(30.0, result[1].totalPlayedSeconds, 0.001)
        assertTrue(result[0].listenDate < result[1].listenDate)
    }

    @Test
    fun dailyListenedTimeIsEmptyWithoutPlayback() = runTest {
        episodeDao.insertBlocking(PodcastEpisode(uuid = "1", publishedDate = Date(), lastPlaybackInteraction = null))

        val result = episodeDao.dailyListenedTime(fromEpochMs = 0)

        assertEquals(emptyList<DailyListenedTime>(), result)
    }
}
