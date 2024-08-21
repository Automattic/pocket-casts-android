package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeDownloadFailureStatistics
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import java.time.Instant
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
        testDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
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

        episodeDao.insert(episode)

        assertEquals(episode, episodeDao.findByUuid(episode.uuid))
    }

    @Test
    fun insertAllAndFindEpisodes() = runBlocking {
        val episodes = listOf(
            PodcastEpisode(uuid = "1", publishedDate = Date()),
            PodcastEpisode(uuid = "2", publishedDate = Date()),
        )

        episodeDao.insertAll(episodes)

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
            episodeStatus = EpisodeStatusEnum.DOWNLOAD_FAILED,
            lastDownloadAttemptDate = Date.from(Instant.EPOCH),
        )
        episodeDao.insert(episode)

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
                episodeStatus = EpisodeStatusEnum.DOWNLOAD_FAILED,
                lastDownloadAttemptDate = Date.from(Instant.EPOCH.plusMillis(index.toLong())),
            )
        }
        episodeDao.insertAll(episodes)

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
                episodeStatus = EpisodeStatusEnum.DOWNLOAD_FAILED,
                lastDownloadAttemptDate = null,
            )
        }
        episodeDao.insertAll(episodes)

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
        val episodes = EpisodeStatusEnum.entries.map { entry ->
            PodcastEpisode(
                uuid = entry.ordinal.toString(),
                publishedDate = Date(),
                episodeStatus = entry,
                lastDownloadAttemptDate = Date.from(Instant.EPOCH.plusMillis(entry.ordinal.toLong())),
            )
        }
        episodeDao.insertAll(episodes)

        val statistics = episodeDao.getFailedDownloadsStatistics()

        val expected = EpisodeDownloadFailureStatistics(
            count = 1,
            newestTimestamp = Instant.EPOCH.plusMillis(EpisodeStatusEnum.DOWNLOAD_FAILED.ordinal.toLong()),
            oldestTimestamp = Instant.EPOCH.plusMillis(EpisodeStatusEnum.DOWNLOAD_FAILED.ordinal.toLong()),
        )
        assertEquals(expected, statistics)
    }
}
