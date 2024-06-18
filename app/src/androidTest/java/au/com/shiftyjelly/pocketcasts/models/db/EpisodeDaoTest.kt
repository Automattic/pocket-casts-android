package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeDownloadFailureStatistics
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherInProgressEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.NovaLauncherNewEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
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

    @Test
    fun getNewReleasesForNovaLauncher() = runTest {
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
            ),
        )
        episodeDao.insertAll(episodes)
        podcastDao.insert(Podcast(uuid = "p-id-1", isSubscribed = true))
        podcastDao.insert(Podcast(uuid = "p-id-2", isSubscribed = true))

        val newEpisodes = episodeDao.getNovaLauncherNewEpisodes(limit = 100)

        val expected = listOf(
            NovaLauncherNewEpisode(
                id = "id-1",
                podcastId = "p-id-1",
                title = "title-1",
                duration = 100,
                currentPosition = 50,
                seasonNumber = 0,
                episodeNumber = 11,
                releaseTimestamp = publishedDate1.time,
                lastUsedTimestamp = 20,
            ),
            NovaLauncherNewEpisode(
                id = "id-2",
                podcastId = "p-id-1",
                title = "title-2",
                duration = 4120,
                currentPosition = 2021,
                seasonNumber = 7,
                episodeNumber = null,
                releaseTimestamp = publishedDate2.time,
                lastUsedTimestamp = null,
            ),
            NovaLauncherNewEpisode(
                id = "id-3",
                podcastId = "p-id-2",
                title = "title-3",
                duration = 2330,
                currentPosition = 0,
                seasonNumber = null,
                episodeNumber = 399,
                releaseTimestamp = publishedDate3.time,
                lastUsedTimestamp = 0,
            ),
        )
        assertEquals(expected, newEpisodes)
    }

    @Test
    fun getNewReleasesForNovaLauncherSortedByReleaseDate() = runTest {
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
        episodeDao.insertAll(episodes)
        podcastDao.insert(Podcast(uuid = "", isSubscribed = true))

        val newEpisodes = episodeDao.getNovaLauncherNewEpisodes(limit = 100)

        val expected = listOf(
            NovaLauncherNewEpisode(
                id = "id-3",
                podcastId = "",
                title = "",
                duration = 0,
                currentPosition = 0,
                seasonNumber = null,
                episodeNumber = null,
                releaseTimestamp = publishedDate3.time,
                lastUsedTimestamp = null,
            ),
            NovaLauncherNewEpisode(
                id = "id-1",
                podcastId = "",
                title = "",
                duration = 0,
                currentPosition = 0,
                seasonNumber = null,
                episodeNumber = null,
                releaseTimestamp = publishedDate1.time,
                lastUsedTimestamp = null,
            ),
            NovaLauncherNewEpisode(
                id = "id-2",
                podcastId = "",
                title = "",
                duration = 0,
                currentPosition = 0,
                seasonNumber = null,
                episodeNumber = null,
                releaseTimestamp = publishedDate2.time,
                lastUsedTimestamp = null,
            ),
        )
        assertEquals(expected, newEpisodes)
    }

    @Test
    fun limitNovaLauncherNewReleases() = runTest {
        val episodes = List(550) {
            PodcastEpisode(
                uuid = "id-$it",
                publishedDate = Date(),
            )
        }
        episodeDao.insertAll(episodes)
        podcastDao.insert(Podcast(uuid = "", isSubscribed = true))

        val newEpisodes = episodeDao.getNovaLauncherNewEpisodes(limit = 75)

        assertEquals(75, newEpisodes.size)
    }

    @Test
    fun ignoreNewReleasesForNovaLauncherThatAreArchived() = runTest {
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
        episodeDao.insertAll(episodes)
        podcastDao.insert(Podcast(uuid = "", isSubscribed = true))

        val newEpisodes = episodeDao.getNovaLauncherNewEpisodes(limit = 100)

        val expected = listOf(
            NovaLauncherNewEpisode(
                id = "id-1",
                podcastId = "",
                title = "",
                duration = 0,
                currentPosition = 0,
                seasonNumber = null,
                episodeNumber = null,
                releaseTimestamp = publishedDate.time,
                lastUsedTimestamp = null,
            ),
        )
        assertEquals(expected, newEpisodes)
    }

    @Test
    fun ignoreNewReleasesForNovaLauncherThatArePlayed() = runTest {
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
        episodeDao.insertAll(episodes)
        podcastDao.insert(Podcast(uuid = "", isSubscribed = true))

        val newEpisodes = episodeDao.getNovaLauncherNewEpisodes(limit = 100)

        val expected = listOf(
            NovaLauncherNewEpisode(
                id = "id-1",
                podcastId = "",
                title = "",
                duration = 0,
                currentPosition = 0,
                seasonNumber = null,
                episodeNumber = null,
                releaseTimestamp = publishedDate.time,
                lastUsedTimestamp = null,
            ),
        )
        assertEquals(expected, newEpisodes)
    }

    @Test
    fun ignoreNewReleasesForNovaLauncherThatAreAtLeastTwoWeeksOld() = runTest {
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
        episodeDao.insertAll(episodes)
        podcastDao.insert(Podcast(uuid = "", isSubscribed = true))

        val newEpisodes = episodeDao.getNovaLauncherNewEpisodes(limit = 100)

        val expected = listOf(
            NovaLauncherNewEpisode(
                id = "id-2",
                podcastId = "",
                title = "",
                duration = 0,
                currentPosition = 0,
                seasonNumber = null,
                episodeNumber = null,
                releaseTimestamp = publishedDate2.time,
                lastUsedTimestamp = null,
            ),
        )
        assertEquals(expected, newEpisodes)
    }

    @Test
    fun ignoreNewReleasesForNovaLauncherForUnsubscribedPodcasts() = runTest {
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
        episodeDao.insertAll(episodes)
        podcastDao.insert(Podcast(uuid = "p-id-1", isSubscribed = true))
        podcastDao.insert(Podcast(uuid = "p-id-2", isSubscribed = false))

        val newEpisodes = episodeDao.getNovaLauncherNewEpisodes(limit = 100)

        val expected = listOf(
            NovaLauncherNewEpisode(
                id = "id-1",
                podcastId = "p-id-1",
                title = "",
                duration = 0,
                currentPosition = 0,
                seasonNumber = null,
                episodeNumber = null,
                releaseTimestamp = publishedDate.time,
                lastUsedTimestamp = null,
            ),
        )
        assertEquals(expected, newEpisodes)
    }

    @Test
    fun getInProgressEpisodesForNovaLauncher() = runTest {
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
                lastPlaybackInteraction = 74211,
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
                publishedDate = Date(2000),
                lastPlaybackInteraction = 0,
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
                publishedDate = Date(3412),
                lastPlaybackInteraction = null,
            ),
        )
        episodeDao.insertAll(episodes)

        val inProgressEpisodes = episodeDao.getNovaLauncherInProgressEpisodes()

        val expected = listOf(
            NovaLauncherInProgressEpisode(
                id = "id-1",
                podcastId = "p-id-1",
                title = "title-1",
                duration = 100,
                currentPosition = 50,
                seasonNumber = 0,
                episodeNumber = 11,
                releaseTimestamp = 0,
                lastUsedTimestamp = 74,
            ),
            NovaLauncherInProgressEpisode(
                id = "id-2",
                podcastId = "p-id-1",
                title = "title-2",
                duration = 4120,
                currentPosition = 2021,
                seasonNumber = 7,
                episodeNumber = null,
                releaseTimestamp = 2,
                lastUsedTimestamp = 0,
            ),
            NovaLauncherInProgressEpisode(
                id = "id-3",
                podcastId = "p-id-2",
                title = "title-3",
                duration = 2330,
                currentPosition = 0,
                seasonNumber = null,
                episodeNumber = 399,
                releaseTimestamp = 3,
                lastUsedTimestamp = null,
            ),
        )
        assertEquals(expected, inProgressEpisodes)
    }

    @Test
    fun getInProgressEpisodesForNovaLauncherSortedByInteractionDate() = runTest {
        val episodes = listOf(
            PodcastEpisode(
                uuid = "id-1",
                playingStatus = EpisodePlayingStatus.IN_PROGRESS,
                publishedDate = Date(2000),
                lastPlaybackInteraction = 1000,
            ),
            PodcastEpisode(
                uuid = "id-2",
                playingStatus = EpisodePlayingStatus.IN_PROGRESS,
                publishedDate = Date(3000),
                lastPlaybackInteraction = null,
            ),
            PodcastEpisode(
                uuid = "id-3",
                playingStatus = EpisodePlayingStatus.IN_PROGRESS,
                publishedDate = Date(1000),
                lastPlaybackInteraction = 3000,
            ),
        )
        episodeDao.insertAll(episodes)

        val inProgressEpisodes = episodeDao.getNovaLauncherInProgressEpisodes()

        val expected = listOf(
            NovaLauncherInProgressEpisode(
                id = "id-3",
                podcastId = "",
                title = "",
                duration = 0,
                currentPosition = 0,
                seasonNumber = null,
                episodeNumber = null,
                releaseTimestamp = 1,
                lastUsedTimestamp = 3,
            ),
            NovaLauncherInProgressEpisode(
                id = "id-1",
                podcastId = "",
                title = "",
                duration = 0,
                currentPosition = 0,
                seasonNumber = null,
                episodeNumber = null,
                releaseTimestamp = 2,
                lastUsedTimestamp = 1,
            ),
            NovaLauncherInProgressEpisode(
                id = "id-2",
                podcastId = "",
                title = "",
                duration = 0,
                currentPosition = 0,
                seasonNumber = null,
                episodeNumber = null,
                releaseTimestamp = 3,
                lastUsedTimestamp = null,
            ),
        )
        assertEquals(expected, inProgressEpisodes)
    }

    @Test
    fun limitNovaLauncherInProgressEpisodesTo500Episodes() = runTest {
        val episodes = List(550) {
            PodcastEpisode(
                uuid = "id-$it",
                publishedDate = Date(),
                playingStatus = EpisodePlayingStatus.IN_PROGRESS,
            )
        }
        episodeDao.insertAll(episodes)

        val inProgressEpisodes = episodeDao.getNovaLauncherInProgressEpisodes()

        assertEquals(500, inProgressEpisodes.size)
    }

    @Test
    fun ignoreInProgressEpisodesForNovaLauncherThatAreArchived() = runTest {
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
        episodeDao.insertAll(episodes)

        val inProgressEpisodes = episodeDao.getNovaLauncherInProgressEpisodes()

        val expected = listOf(
            NovaLauncherInProgressEpisode(
                id = "id-1",
                podcastId = "",
                title = "",
                duration = 0,
                currentPosition = 0,
                seasonNumber = null,
                episodeNumber = null,
                releaseTimestamp = 0,
                lastUsedTimestamp = null,
            ),
        )
        assertEquals(expected, inProgressEpisodes)
    }

    @Test
    fun ignoreInProgressEpisodesForNovaLauncherThatAreNotInProgress() = runTest {
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
        episodeDao.insertAll(episodes)

        val inProgressEpisodes = episodeDao.getNovaLauncherInProgressEpisodes()

        val expected = listOf(
            NovaLauncherInProgressEpisode(
                id = "id-1",
                podcastId = "",
                title = "",
                duration = 0,
                currentPosition = 0,
                seasonNumber = null,
                episodeNumber = null,
                releaseTimestamp = 0,
                lastUsedTimestamp = null,
            ),
        )
        assertEquals(expected, inProgressEpisodes)
    }
}
