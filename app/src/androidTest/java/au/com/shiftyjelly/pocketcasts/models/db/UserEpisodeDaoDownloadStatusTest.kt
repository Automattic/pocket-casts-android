package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.UserEpisodeDao
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.DownloadStatusUpdate
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import com.squareup.moshi.Moshi
import java.io.File
import java.time.Instant
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserEpisodeDaoDownloadStatusTest {
    lateinit var userEpisodeDao: UserEpisodeDao
    lateinit var testDb: AppDatabase

    private val workerId = UUID.randomUUID()
    private val now = Instant.now()
    private val nowDate = Date.from(now)
    private val previousDate = Date.from(now.minusSeconds(100))
    private val episode = UserEpisode(
        uuid = "episode-id",
        publishedDate = Date(),
        // Prepare episode with some data to verify correct state in tests
        downloadStatus = EpisodeDownloadStatus.DownloadNotRequested,
        downloadedFilePath = "invalid_path",
        downloadErrorDetails = "invalid_details",
        downloadTaskId = workerId.toString(),
        lastDownloadAttemptDate = previousDate,
    )

    @Before
    fun setupDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(ModelModule.provideRoomConverters(Moshi.Builder().build()))
            .build()
        userEpisodeDao = testDb.userEpisodeDao()
        runBlocking { userEpisodeDao.insert(episode) }
    }

    @After
    fun closeDb() {
        testDb.close()
    }

    @Test
    fun updateDownloadStatusToCancelled() = runTest {
        userEpisodeDao.update(episode.copy(downloadStatus = EpisodeDownloadStatus.Downloading))

        val isStatusUpdated = userEpisodeDao.updateDownloadStatus(episode.uuid, DownloadStatusUpdate.Cancelled(workerId, now))
        assertEquals(true, isStatusUpdated)

        val updatedEpisode = userEpisodeDao.findEpisodeByUuid(episode.uuid)!!
        assertEquals(EpisodeDownloadStatus.DownloadNotRequested, updatedEpisode.downloadStatus)
        assertEquals(previousDate, updatedEpisode.lastDownloadAttemptDate)
        assertEquals(null, updatedEpisode.downloadedFilePath)
        assertEquals(null, updatedEpisode.downloadErrorDetails)
        assertEquals(null, updatedEpisode.downloadTaskId)
    }

    @Test
    fun updateDownloadStatusToWaitingForWifi() = runTest {
        val update = DownloadStatusUpdate.WaitingForWifi(workerId, now)
        val isStatusUpdated = userEpisodeDao.updateDownloadStatus(episode.uuid, update)
        assertEquals(true, isStatusUpdated)

        val updatedEpisode = userEpisodeDao.findEpisodeByUuid(episode.uuid)!!
        assertEquals(EpisodeDownloadStatus.WaitingForWifi, updatedEpisode.downloadStatus)
        assertEquals(previousDate, updatedEpisode.lastDownloadAttemptDate)
        assertEquals(null, updatedEpisode.downloadedFilePath)
        assertEquals(null, updatedEpisode.downloadErrorDetails)
        assertEquals(workerId.toString(), updatedEpisode.downloadTaskId)
    }

    @Test
    fun updateDownloadStatusToWaitingForPower() = runTest {
        val update = DownloadStatusUpdate.WaitingForPower(workerId, now)
        val isStatusUpdated = userEpisodeDao.updateDownloadStatus(episode.uuid, update)
        assertEquals(true, isStatusUpdated)

        val updatedEpisode = userEpisodeDao.findEpisodeByUuid(episode.uuid)!!
        assertEquals(EpisodeDownloadStatus.WaitingForPower, updatedEpisode.downloadStatus)
        assertEquals(previousDate, updatedEpisode.lastDownloadAttemptDate)
        assertEquals(null, updatedEpisode.downloadedFilePath)
        assertEquals(null, updatedEpisode.downloadErrorDetails)
        assertEquals(workerId.toString(), updatedEpisode.downloadTaskId)
    }

    @Test
    fun updateDownloadStatusToWaitingForStorage() = runTest {
        val update = DownloadStatusUpdate.WaitingForStorage(workerId, now)
        val isStatusUpdated = userEpisodeDao.updateDownloadStatus(episode.uuid, update)
        assertEquals(true, isStatusUpdated)

        val updatedEpisode = userEpisodeDao.findEpisodeByUuid(episode.uuid)!!
        assertEquals(EpisodeDownloadStatus.WaitingForStorage, updatedEpisode.downloadStatus)
        assertEquals(previousDate, updatedEpisode.lastDownloadAttemptDate)
        assertEquals(null, updatedEpisode.downloadedFilePath)
        assertEquals(null, updatedEpisode.downloadErrorDetails)
        assertEquals(workerId.toString(), updatedEpisode.downloadTaskId)
    }

    @Test
    fun updateDownloadStatusToEnqueued() = runTest {
        val update = DownloadStatusUpdate.Enqueued(workerId, now)
        val isStatusUpdated = userEpisodeDao.updateDownloadStatus(episode.uuid, update)
        assertEquals(true, isStatusUpdated)

        val updatedEpisode = userEpisodeDao.findEpisodeByUuid(episode.uuid)!!
        assertEquals(EpisodeDownloadStatus.Queued, updatedEpisode.downloadStatus)
        assertEquals(previousDate, updatedEpisode.lastDownloadAttemptDate)
        assertEquals(null, updatedEpisode.downloadedFilePath)
        assertEquals(null, updatedEpisode.downloadErrorDetails)
        assertEquals(workerId.toString(), updatedEpisode.downloadTaskId)
    }

    @Test
    fun updateDownloadStatusToInProgress() = runTest {
        val update = DownloadStatusUpdate.InProgress(workerId, now)
        val isStatusUpdated = userEpisodeDao.updateDownloadStatus(episode.uuid, update)
        assertEquals(true, isStatusUpdated)

        val updatedEpisode = userEpisodeDao.findEpisodeByUuid(episode.uuid)!!
        assertEquals(EpisodeDownloadStatus.Downloading, updatedEpisode.downloadStatus)
        assertEquals(nowDate, updatedEpisode.lastDownloadAttemptDate)
        assertEquals(null, updatedEpisode.downloadedFilePath)
        assertEquals(null, updatedEpisode.downloadErrorDetails)
        assertEquals(workerId.toString(), updatedEpisode.downloadTaskId)
    }

    @Test
    fun updateDownloadStatusToSuccess() = runTest {
        val file = File("podcast.mp3")
        val update = DownloadStatusUpdate.Success(workerId, now, file)
        val isStatusUpdated = userEpisodeDao.updateDownloadStatus(episode.uuid, update)
        assertEquals(true, isStatusUpdated)

        val updatedEpisode = userEpisodeDao.findEpisodeByUuid(episode.uuid)!!
        assertEquals(EpisodeDownloadStatus.Downloaded, updatedEpisode.downloadStatus)
        assertEquals(previousDate, updatedEpisode.lastDownloadAttemptDate)
        assertEquals(file.path, updatedEpisode.downloadedFilePath)
        assertEquals(null, updatedEpisode.downloadErrorDetails)
        assertEquals(null, updatedEpisode.downloadTaskId)
    }

    @Test
    fun updateDownloadStatusToFailure() = runTest {
        val errorMessage = "Download failed"
        val update = DownloadStatusUpdate.Failure(workerId, now, errorMessage)
        val isStatusUpdated = userEpisodeDao.updateDownloadStatus(episode.uuid, update)
        assertEquals(true, isStatusUpdated)

        val updatedEpisode = userEpisodeDao.findEpisodeByUuid(episode.uuid)!!
        assertEquals(EpisodeDownloadStatus.DownloadFailed, updatedEpisode.downloadStatus)
        assertEquals(previousDate, updatedEpisode.lastDownloadAttemptDate)
        assertEquals(null, updatedEpisode.downloadedFilePath)
        assertEquals(errorMessage, updatedEpisode.downloadErrorDetails)
        assertEquals(null, updatedEpisode.downloadTaskId)
    }

    @Test
    fun doNotUpdateStatesWithDifferentWorkerId() = runTest {
        val episode = episode.copy(downloadTaskId = UUID.randomUUID().toString())
        userEpisodeDao.update(episode)

        val updates = listOf(
            DownloadStatusUpdate.Cancelled(workerId, now),
            DownloadStatusUpdate.WaitingForWifi(workerId, now),
            DownloadStatusUpdate.WaitingForPower(workerId, now),
            DownloadStatusUpdate.WaitingForStorage(workerId, now),
            DownloadStatusUpdate.Enqueued(workerId, now),
            DownloadStatusUpdate.InProgress(workerId, now),
            DownloadStatusUpdate.Success(workerId, now, File("audio.mp3")),
            DownloadStatusUpdate.Failure(workerId, now, "error_message"),
        )

        for (update in updates) {
            val isStatusUpdated = userEpisodeDao.updateDownloadStatus(episode.uuid, update)
            assertEquals(false, isStatusUpdated)

            val updatedEpisode = userEpisodeDao.findEpisodeByUuid(episode.uuid)!!
            assertEquals(episode, updatedEpisode)
        }
    }

    @Test
    fun setReadyForDownloadWithoutWorkerId() = runTest {
        userEpisodeDao.update(
            episode.copy(
                isArchived = true,
                downloadStatus = EpisodeDownloadStatus.DownloadNotRequested,
                downloadTaskId = null,
                autoDownloadStatus = PodcastEpisode.AUTO_DOWNLOAD_STATUS_IGNORE,
            ),
        )

        val isStatusUpdated = userEpisodeDao.setReadyForDownload(episode.uuid, workerId, now, forceNewDownload = false)
        assertEquals(true, isStatusUpdated)

        val updatedEpisode = userEpisodeDao.findEpisodeByUuid(episode.uuid)!!
        assertEquals(false, updatedEpisode.isArchived)
        assertEquals(EpisodeDownloadStatus.Queued, updatedEpisode.downloadStatus)
        assertEquals(nowDate, updatedEpisode.lastDownloadAttemptDate)
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
        userEpisodeDao.update(episode)

        val isStatusUpdated = userEpisodeDao.setReadyForDownload(episode.uuid, workerId, now, forceNewDownload = false)
        assertEquals(false, isStatusUpdated)

        val updatedEpisode = userEpisodeDao.findEpisodeByUuid(episode.uuid)!!
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
        userEpisodeDao.update(episode)

        val isStatusUpdated = userEpisodeDao.setReadyForDownload(episode.uuid, workerId, now, forceNewDownload = true)
        assertEquals(true, isStatusUpdated)

        val updatedEpisode = userEpisodeDao.findEpisodeByUuid(episode.uuid)!!
        assertEquals(false, updatedEpisode.isArchived)
        assertEquals(nowDate, updatedEpisode.lastDownloadAttemptDate)
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
        userEpisodeDao.update(episode)

        val isStatusUpdated = userEpisodeDao.setDownloadCancelled(episode.uuid, disableAutoDownload = true)
        assertEquals(true, isStatusUpdated)

        val updatedEpisode = userEpisodeDao.findEpisodeByUuid(episode.uuid)!!
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
        userEpisodeDao.update(episode)

        val isStatusUpdated = userEpisodeDao.setDownloadCancelled(episode.uuid, disableAutoDownload = false)
        assertEquals(true, isStatusUpdated)

        val updatedEpisode = userEpisodeDao.findEpisodeByUuid(episode.uuid)!!
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
        userEpisodeDao.update(episode)

        val isStatusUpdated = userEpisodeDao.setDownloadCancelled(episode.uuid, disableAutoDownload = true)
        assertEquals(false, isStatusUpdated)

        val updatedEpisode = userEpisodeDao.findEpisodeByUuid(episode.uuid)!!
        assertEquals(episode, updatedEpisode)
    }
}
