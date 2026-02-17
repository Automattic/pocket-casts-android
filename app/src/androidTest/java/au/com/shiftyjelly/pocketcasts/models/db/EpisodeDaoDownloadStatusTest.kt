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
import java.util.UUID
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

    private val workerId = UUID.randomUUID()
    private val episode = PodcastEpisode(
        uuid = "episode-id",
        publishedDate = Date(),
        // Prepare episode with some data to verify correct state in tests
        downloadStatus = EpisodeDownloadStatus.DownloadNotRequested,
        downloadedFilePath = "invalid_path",
        downloadErrorDetails = "invalid_details",
        downloadTaskId = workerId.toString(),
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
    fun updateDownloadStatusToCancelled() = runTest {
        episodeDao.update(episode.copy(downloadStatus = EpisodeDownloadStatus.Downloading))

        val isStatusUpdated = episodeDao.updateDownloadStatus(episode.uuid, DownloadStatusUpdate.Cancelled(workerId))
        assertEquals(true, isStatusUpdated)

        val updatedEpisode = episodeDao.findByUuid(episode.uuid)!!
        assertEquals(EpisodeDownloadStatus.DownloadNotRequested, updatedEpisode.downloadStatus)
        assertEquals(null, updatedEpisode.downloadedFilePath)
        assertEquals(null, updatedEpisode.downloadErrorDetails)
        assertEquals(null, updatedEpisode.downloadTaskId)
    }

    @Test
    fun updateDownloadStatusToWaitingForWifi() = runTest {
        val update = DownloadStatusUpdate.WaitingForWifi(workerId)
        val isStatusUpdated = episodeDao.updateDownloadStatus(episode.uuid, update)
        assertEquals(true, isStatusUpdated)

        val updatedEpisode = episodeDao.findByUuid(episode.uuid)!!
        assertEquals(EpisodeDownloadStatus.WaitingForWifi, updatedEpisode.downloadStatus)
        assertEquals(null, updatedEpisode.downloadedFilePath)
        assertEquals(null, updatedEpisode.downloadErrorDetails)
        assertEquals(workerId.toString(), updatedEpisode.downloadTaskId)
    }

    @Test
    fun updateDownloadStatusToWaitingForPower() = runTest {
        val update = DownloadStatusUpdate.WaitingForPower(workerId)
        val isStatusUpdated = episodeDao.updateDownloadStatus(episode.uuid, update)
        assertEquals(true, isStatusUpdated)

        val updatedEpisode = episodeDao.findByUuid(episode.uuid)!!
        assertEquals(EpisodeDownloadStatus.WaitingForPower, updatedEpisode.downloadStatus)
        assertEquals(null, updatedEpisode.downloadedFilePath)
        assertEquals(null, updatedEpisode.downloadErrorDetails)
        assertEquals(workerId.toString(), updatedEpisode.downloadTaskId)
    }

    @Test
    fun updateDownloadStatusToWaitingForStorage() = runTest {
        val update = DownloadStatusUpdate.WaitingForStorage(workerId)
        val isStatusUpdated = episodeDao.updateDownloadStatus(episode.uuid, update)
        assertEquals(true, isStatusUpdated)

        val updatedEpisode = episodeDao.findByUuid(episode.uuid)!!
        assertEquals(EpisodeDownloadStatus.WaitingForStorage, updatedEpisode.downloadStatus)
        assertEquals(null, updatedEpisode.downloadedFilePath)
        assertEquals(null, updatedEpisode.downloadErrorDetails)
        assertEquals(workerId.toString(), updatedEpisode.downloadTaskId)
    }

    @Test
    fun updateDownloadStatusToEnqueued() = runTest {
        val update = DownloadStatusUpdate.Enqueued(workerId)
        val isStatusUpdated = episodeDao.updateDownloadStatus(episode.uuid, update)
        assertEquals(true, isStatusUpdated)

        val updatedEpisode = episodeDao.findByUuid(episode.uuid)!!
        assertEquals(EpisodeDownloadStatus.Queued, updatedEpisode.downloadStatus)
        assertEquals(null, updatedEpisode.downloadedFilePath)
        assertEquals(null, updatedEpisode.downloadErrorDetails)
        assertEquals(workerId.toString(), updatedEpisode.downloadTaskId)
    }

    @Test
    fun updateDownloadStatusToInProgress() = runTest {
        val update = DownloadStatusUpdate.InProgress(workerId)
        val isStatusUpdated = episodeDao.updateDownloadStatus(episode.uuid, update)
        assertEquals(true, isStatusUpdated)

        val updatedEpisode = episodeDao.findByUuid(episode.uuid)!!
        assertEquals(EpisodeDownloadStatus.Downloading, updatedEpisode.downloadStatus)
        assertEquals(null, updatedEpisode.downloadedFilePath)
        assertEquals(null, updatedEpisode.downloadErrorDetails)
        assertEquals(workerId.toString(), updatedEpisode.downloadTaskId)
    }

    @Test
    fun updateDownloadStatusToSuccess() = runTest {
        val file = File("podcast.mp3")
        val update = DownloadStatusUpdate.Success(workerId, file)
        val isStatusUpdated = episodeDao.updateDownloadStatus(episode.uuid, update)
        assertEquals(true, isStatusUpdated)

        val updatedEpisode = episodeDao.findByUuid(episode.uuid)!!
        assertEquals(EpisodeDownloadStatus.Downloaded, updatedEpisode.downloadStatus)
        assertEquals(file.path, updatedEpisode.downloadedFilePath)
        assertEquals(null, updatedEpisode.downloadErrorDetails)
        assertEquals(null, updatedEpisode.downloadTaskId)
    }

    @Test
    fun updateDownloadStatusToFailure() = runTest {
        val errorMessage = "Download failed"
        val update = DownloadStatusUpdate.Failure(workerId, errorMessage)
        val isStatusUpdated = episodeDao.updateDownloadStatus(episode.uuid, update)
        assertEquals(true, isStatusUpdated)

        val updatedEpisode = episodeDao.findByUuid(episode.uuid)!!
        assertEquals(EpisodeDownloadStatus.DownloadFailed, updatedEpisode.downloadStatus)
        assertEquals(null, updatedEpisode.downloadedFilePath)
        assertEquals(errorMessage, updatedEpisode.downloadErrorDetails)
        assertEquals(null, updatedEpisode.downloadTaskId)
    }

    @Test
    fun doNotUpdateStatesWithDifferentWorkerId() = runTest {
        val episode = episode.copy(downloadTaskId = UUID.randomUUID().toString())
        episodeDao.update(episode)

        val updates = listOf(
            DownloadStatusUpdate.Cancelled(workerId),
            DownloadStatusUpdate.WaitingForWifi(workerId),
            DownloadStatusUpdate.WaitingForPower(workerId),
            DownloadStatusUpdate.WaitingForStorage(workerId),
            DownloadStatusUpdate.Enqueued(workerId),
            DownloadStatusUpdate.InProgress(workerId),
            DownloadStatusUpdate.Success(workerId, File("audio.mp3")),
            DownloadStatusUpdate.Failure(workerId, "error_message"),
        )

        for (update in updates) {
            val isStatusUpdated = episodeDao.updateDownloadStatus(episode.uuid, update)
            assertEquals(false, isStatusUpdated)

            val updatedEpisode = episodeDao.findByUuid(episode.uuid)!!
            assertEquals(episode, updatedEpisode)
        }
    }

    @Test
    fun setReadyForDownloadWithoutWorkerId() = runTest {
        episodeDao.update(
            episode.copy(
                isArchived = true,
                downloadStatus = EpisodeDownloadStatus.DownloadNotRequested,
                downloadTaskId = null,
                autoDownloadStatus = PodcastEpisode.AUTO_DOWNLOAD_STATUS_IGNORE,
            ),
        )

        val isStatusUpdated = episodeDao.setReadyForDownload(episode.uuid, workerId, forceNewDownload = false)
        assertEquals(true, isStatusUpdated)

        val updatedEpisode = episodeDao.findByUuid(episode.uuid)!!
        assertEquals(false, updatedEpisode.isArchived)
        assertEquals(EpisodeDownloadStatus.Queued, updatedEpisode.downloadStatus)
        assertEquals(workerId.toString(), updatedEpisode.downloadTaskId)
        assertEquals(false, updatedEpisode.isExemptFromAutoDownload)
    }

    @Test
    fun setReadyForDownloadWithWorkerId() = runTest {
        val episode = episode.copy(
            isArchived = true,
            downloadStatus = EpisodeDownloadStatus.DownloadNotRequested,
            downloadTaskId = UUID.randomUUID().toString(),
            autoDownloadStatus = PodcastEpisode.AUTO_DOWNLOAD_STATUS_IGNORE,
        )
        episodeDao.update(episode)

        val isStatusUpdated = episodeDao.setReadyForDownload(episode.uuid, workerId, forceNewDownload = false)
        assertEquals(false, isStatusUpdated)

        val updatedEpisode = episodeDao.findByUuid(episode.uuid)!!
        assertEquals(episode, updatedEpisode)
    }

    @Test
    fun setReadyForDownloadWithWorkerIdWhenForced() = runTest {
        val episode = episode.copy(
            isArchived = true,
            downloadStatus = EpisodeDownloadStatus.DownloadNotRequested,
            downloadTaskId = UUID.randomUUID().toString(),
            autoDownloadStatus = PodcastEpisode.AUTO_DOWNLOAD_STATUS_IGNORE,
        )
        episodeDao.update(episode)

        val isStatusUpdated = episodeDao.setReadyForDownload(episode.uuid, workerId, forceNewDownload = true)
        assertEquals(true, isStatusUpdated)

        val updatedEpisode = episodeDao.findByUuid(episode.uuid)!!
        assertEquals(false, updatedEpisode.isArchived)
        assertEquals(EpisodeDownloadStatus.Queued, updatedEpisode.downloadStatus)
        assertEquals(workerId.toString(), updatedEpisode.downloadTaskId)
        assertEquals(false, updatedEpisode.isExemptFromAutoDownload)
    }

    @Test
    fun setDownloadCancelledWithDisabledAutoDownloads() = runTest {
        val episode = episode.copy(
            downloadStatus = EpisodeDownloadStatus.Downloading,
            autoDownloadStatus = PodcastEpisode.AUTO_DOWNLOAD_STATUS_MANUALLY_DOWNLOADED,
        )
        episodeDao.update(episode)

        val isStatusUpdated = episodeDao.setDownloadCancelled(episode.uuid, disableAutoDownload = true)
        assertEquals(true, isStatusUpdated)

        val updatedEpisode = episodeDao.findByUuid(episode.uuid)!!
        assertEquals(EpisodeDownloadStatus.DownloadNotRequested, updatedEpisode.downloadStatus)
        assertEquals(true, updatedEpisode.isExemptFromAutoDownload)
        assertEquals(null, updatedEpisode.downloadTaskId)
    }

    @Test
    fun setDownloadCancelledWithoutDisabledAutoDownloads() = runTest {
        val episode = episode.copy(
            downloadStatus = EpisodeDownloadStatus.Downloading,
            autoDownloadStatus = PodcastEpisode.AUTO_DOWNLOAD_STATUS_MANUALLY_DOWNLOADED,
        )
        episodeDao.update(episode)

        val isStatusUpdated = episodeDao.setDownloadCancelled(episode.uuid, disableAutoDownload = false)
        assertEquals(true, isStatusUpdated)

        val updatedEpisode = episodeDao.findByUuid(episode.uuid)!!
        assertEquals(EpisodeDownloadStatus.DownloadNotRequested, updatedEpisode.downloadStatus)
        assertEquals(false, updatedEpisode.isExemptFromAutoDownload)
        assertEquals(null, updatedEpisode.downloadTaskId)
    }

    @Test
    fun setDownloadCancelledWithoutWorkerId() = runTest {
        val episode = episode.copy(
            downloadStatus = EpisodeDownloadStatus.Downloading,
            autoDownloadStatus = PodcastEpisode.AUTO_DOWNLOAD_STATUS_MANUALLY_DOWNLOADED,
            downloadTaskId = null,
        )
        episodeDao.update(episode)

        val isStatusUpdated = episodeDao.setDownloadCancelled(episode.uuid, disableAutoDownload = true)
        assertEquals(false, isStatusUpdated)

        val updatedEpisode = episodeDao.findByUuid(episode.uuid)!!
        assertEquals(episode, updatedEpisode)
    }
}
