package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.DownloadStatusUpdate
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import com.squareup.moshi.Moshi
import java.io.File
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EpisodeDaoDownloadStatusTest {
    lateinit var episodeDao: EpisodeDao
    lateinit var testDb: AppDatabase

    private val episode = PodcastEpisode(
        uuid = "episode-id",
        publishedDate = Date(),
        // Prepare episode with some data to verify correct state in tests
        downloadStatus = EpisodeDownloadStatus.DownloadNotRequested,
        downloadedFilePath = "invalid_path",
        downloadErrorDetails = "invalid_details",
    )

    @Before
    fun setupDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(ModelModule.provideRoomConverters(Moshi.Builder().build()))
            .build()
        episodeDao = testDb.episodeDao()
        episodeDao.insertBlocking(episode)
    }

    @After
    fun closeDb() {
        testDb.close()
    }

    @Test
    fun updateDownloadStatusToIdle() = runTest {
        episodeDao.updateEpisodeStatus(EpisodeDownloadStatus.Downloading, episode.uuid)

        episodeDao.updateDownloadStatuses(mapOf(episode.uuid to DownloadStatusUpdate.Idle))

        val result = episodeDao.findByUuid(episode.uuid)!!

        assertEquals(EpisodeDownloadStatus.DownloadNotRequested, result.downloadStatus)
        assertEquals(null, result.downloadedFilePath)
        assertEquals(null, result.downloadErrorDetails)
    }

    @Test
    fun updateDownloadStatusToWaitingForWifi() = runTest {
        episodeDao.updateDownloadStatuses(mapOf(episode.uuid to DownloadStatusUpdate.WaitingForWifi))

        val result = episodeDao.findByUuid(episode.uuid)!!

        assertEquals(EpisodeDownloadStatus.WaitingForWifi, result.downloadStatus)
        assertEquals(null, result.downloadedFilePath)
        assertEquals(null, result.downloadErrorDetails)
    }

    @Test
    fun updateDownloadStatusToWaitingForPower() = runTest {
        episodeDao.updateDownloadStatuses(mapOf(episode.uuid to DownloadStatusUpdate.WaitingForPower))

        val result = episodeDao.findByUuid(episode.uuid)!!

        assertEquals(EpisodeDownloadStatus.WaitingForPower, result.downloadStatus)
        assertEquals(null, result.downloadedFilePath)
        assertEquals(null, result.downloadErrorDetails)
    }

    @Test
    fun updateDownloadStatusToWaitingForStorage() = runTest {
        episodeDao.updateDownloadStatuses(mapOf(episode.uuid to DownloadStatusUpdate.WaitingForStorage))

        val result = episodeDao.findByUuid(episode.uuid)!!

        assertEquals(EpisodeDownloadStatus.WaitingForStorage, result.downloadStatus)
        assertEquals(null, result.downloadedFilePath)
        assertEquals(null, result.downloadErrorDetails)
    }

    @Test
    fun updateDownloadStatusToQueued() = runTest {
        episodeDao.updateDownloadStatuses(mapOf(episode.uuid to DownloadStatusUpdate.Queued))

        val result = episodeDao.findByUuid(episode.uuid)!!

        assertEquals(EpisodeDownloadStatus.Queued, result.downloadStatus)
        assertEquals(null, result.downloadedFilePath)
        assertEquals(null, result.downloadErrorDetails)
    }

    @Test
    fun updateDownloadStatusToQueuedRetry() = runTest {
        episodeDao.updateDownloadStatuses(mapOf(episode.uuid to DownloadStatusUpdate.QueuedRetry))

        val result = episodeDao.findByUuid(episode.uuid)!!

        assertEquals(EpisodeDownloadStatus.QueuedRetry, result.downloadStatus)
        assertEquals(null, result.downloadedFilePath)
        assertEquals(null, result.downloadErrorDetails)
    }

    @Test
    fun updateDownloadStatusToInProgress() = runTest {
        episodeDao.updateDownloadStatuses(mapOf(episode.uuid to DownloadStatusUpdate.InProgress))

        val result = episodeDao.findByUuid(episode.uuid)!!

        assertEquals(EpisodeDownloadStatus.Downloading, result.downloadStatus)
        assertEquals(null, result.downloadedFilePath)
        assertEquals(null, result.downloadErrorDetails)
    }

    @Test
    fun updateDownloadStatusToSuccess() = runTest {
        val file = File("podcast.mp3")
        episodeDao.updateDownloadStatuses(mapOf(episode.uuid to DownloadStatusUpdate.Success(file)))

        val result = episodeDao.findByUuid(episode.uuid)!!

        assertEquals(EpisodeDownloadStatus.Downloaded, result.downloadStatus)
        assertEquals(file.path, result.downloadedFilePath)
        assertEquals(null, result.downloadErrorDetails)
    }

    @Test
    fun updateDownloadStatusToFailure() = runTest {
        val errorMessage = "Download failed"
        episodeDao.updateDownloadStatuses(mapOf(episode.uuid to DownloadStatusUpdate.Failure(errorMessage)))

        val result = episodeDao.findByUuid(episode.uuid)!!

        assertEquals(EpisodeDownloadStatus.DownloadFailed, result.downloadStatus)
        assertEquals(null, result.downloadedFilePath)
        assertEquals(errorMessage, result.downloadErrorDetails)
    }

    @Test
    fun updateMultipleEpisodes() = runTest {
        episodeDao.insertAllBlocking(
            listOf(
                PodcastEpisode(
                    uuid = "id-1",
                    publishedDate = Date(),
                    downloadStatus = EpisodeDownloadStatus.DownloadNotRequested,
                    downloadedFilePath = "invalid_path",
                    downloadErrorDetails = "invalid_details",
                ),
                PodcastEpisode(
                    uuid = "id-2",
                    publishedDate = Date(),
                    downloadStatus = EpisodeDownloadStatus.DownloadNotRequested,
                    downloadedFilePath = "invalid_path",
                    downloadErrorDetails = "invalid_details",
                ),
            ),
        )

        episodeDao.updateDownloadStatuses(
            mapOf(
                "id-1" to DownloadStatusUpdate.InProgress,
                "id-2" to DownloadStatusUpdate.Success(File("audio.mp3")),
            ),
        )

        val result1 = episodeDao.findByUuid("id-1")!!
        val result2 = episodeDao.findByUuid("id-2")!!

        assertEquals(EpisodeDownloadStatus.Downloading, result1.downloadStatus)
        assertEquals(null, result1.downloadedFilePath)
        assertEquals(null, result1.downloadErrorDetails)

        assertEquals(EpisodeDownloadStatus.Downloaded, result2.downloadStatus)
        assertEquals("audio.mp3", result2.downloadedFilePath)
        assertEquals(null, result2.downloadErrorDetails)
    }
}
