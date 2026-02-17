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
    fun updateDownloadStatusToIdle() = runTest {
        episodeDao.updateEpisodeStatus(EpisodeDownloadStatus.Downloading, episode.uuid)
        episodeDao.updateDownloadStatuses(mapOf(episode.uuid to DownloadStatusUpdate.Idle))

        val result = episodeDao.findByUuid(episode.uuid)!!

        assertEquals(EpisodeDownloadStatus.DownloadNotRequested, result.downloadStatus)
        assertEquals(null, result.downloadedFilePath)
        assertEquals(null, result.downloadErrorDetails)
        assertEquals(null, result.downloadTaskId)
    }

    @Test
    fun updateDownloadStatusToWaitingForWifi() = runTest {
        val update = DownloadStatusUpdate.WaitingForWifi(workerId)
        episodeDao.updateDownloadStatuses(mapOf(episode.uuid to update))

        val result = episodeDao.findByUuid(episode.uuid)!!

        assertEquals(EpisodeDownloadStatus.WaitingForWifi, result.downloadStatus)
        assertEquals(null, result.downloadedFilePath)
        assertEquals(null, result.downloadErrorDetails)
        assertEquals(workerId.toString(), result.downloadTaskId)
    }

    @Test
    fun updateDownloadStatusToWaitingForPower() = runTest {
        val update = DownloadStatusUpdate.WaitingForPower(workerId)
        episodeDao.updateDownloadStatuses(mapOf(episode.uuid to update))

        val result = episodeDao.findByUuid(episode.uuid)!!

        assertEquals(EpisodeDownloadStatus.WaitingForPower, result.downloadStatus)
        assertEquals(null, result.downloadedFilePath)
        assertEquals(null, result.downloadErrorDetails)
        assertEquals(workerId.toString(), result.downloadTaskId)
    }

    @Test
    fun updateDownloadStatusToWaitingForStorage() = runTest {
        val update = DownloadStatusUpdate.WaitingForStorage(workerId)
        episodeDao.updateDownloadStatuses(mapOf(episode.uuid to update))

        val result = episodeDao.findByUuid(episode.uuid)!!

        assertEquals(EpisodeDownloadStatus.WaitingForStorage, result.downloadStatus)
        assertEquals(null, result.downloadedFilePath)
        assertEquals(null, result.downloadErrorDetails)
        assertEquals(workerId.toString(), result.downloadTaskId)
    }

    @Test
    fun updateDownloadStatusToEnqueued() = runTest {
        val update = DownloadStatusUpdate.Enqueued(workerId)
        episodeDao.updateDownloadStatuses(mapOf(episode.uuid to update))

        val result = episodeDao.findByUuid(episode.uuid)!!

        assertEquals(EpisodeDownloadStatus.Queued, result.downloadStatus)
        assertEquals(null, result.downloadedFilePath)
        assertEquals(null, result.downloadErrorDetails)
        assertEquals(workerId.toString(), result.downloadTaskId)
    }

    @Test
    fun updateDownloadStatusToInProgress() = runTest {
        val update = DownloadStatusUpdate.InProgress(workerId)
        episodeDao.updateDownloadStatuses(mapOf(episode.uuid to update))

        val result = episodeDao.findByUuid(episode.uuid)!!

        assertEquals(EpisodeDownloadStatus.Downloading, result.downloadStatus)
        assertEquals(null, result.downloadedFilePath)
        assertEquals(null, result.downloadErrorDetails)
        assertEquals(workerId.toString(), result.downloadTaskId)
    }

    @Test
    fun updateDownloadStatusToSuccess() = runTest {
        val file = File("podcast.mp3")
        val update = DownloadStatusUpdate.Success(workerId, file)
        episodeDao.updateDownloadStatuses(mapOf(episode.uuid to update))

        val result = episodeDao.findByUuid(episode.uuid)!!

        assertEquals(EpisodeDownloadStatus.Downloaded, result.downloadStatus)
        assertEquals(file.path, result.downloadedFilePath)
        assertEquals(null, result.downloadErrorDetails)
        assertEquals(null, result.downloadTaskId)
    }

    @Test
    fun updateDownloadStatusToFailure() = runTest {
        val errorMessage = "Download failed"
        val update = DownloadStatusUpdate.Failure(workerId, errorMessage)
        episodeDao.updateDownloadStatuses(mapOf(episode.uuid to update))

        val result = episodeDao.findByUuid(episode.uuid)!!

        assertEquals(EpisodeDownloadStatus.DownloadFailed, result.downloadStatus)
        assertEquals(null, result.downloadedFilePath)
        assertEquals(errorMessage, result.downloadErrorDetails)
        assertEquals(null, result.downloadTaskId)
    }

    @Test
    fun updateMultipleEpisodes() = runTest {
        val workerId1 = UUID.randomUUID()
        val workerId2 = UUID.randomUUID()

        episodeDao.insertAllBlocking(
            listOf(
                PodcastEpisode(
                    uuid = "id-1",
                    publishedDate = Date(),
                    downloadStatus = EpisodeDownloadStatus.DownloadNotRequested,
                    downloadedFilePath = "invalid_path",
                    downloadErrorDetails = "invalid_details",
                    downloadTaskId = workerId1.toString(),
                ),
                PodcastEpisode(
                    uuid = "id-2",
                    publishedDate = Date(),
                    downloadStatus = EpisodeDownloadStatus.DownloadNotRequested,
                    downloadedFilePath = "invalid_path",
                    downloadErrorDetails = "invalid_details",
                    downloadTaskId = workerId2.toString(),
                ),
            ),
        )

        episodeDao.updateDownloadStatuses(
            mapOf(
                "id-1" to DownloadStatusUpdate.InProgress(workerId1),
                "id-2" to DownloadStatusUpdate.Success(workerId2, File("audio.mp3")),
            ),
        )

        val result1 = episodeDao.findByUuid("id-1")!!
        val result2 = episodeDao.findByUuid("id-2")!!

        assertEquals(EpisodeDownloadStatus.Downloading, result1.downloadStatus)
        assertEquals(null, result1.downloadedFilePath)
        assertEquals(null, result1.downloadErrorDetails)
        assertEquals(workerId1.toString(), result1.downloadTaskId)

        assertEquals(EpisodeDownloadStatus.Downloaded, result2.downloadStatus)
        assertEquals("audio.mp3", result2.downloadedFilePath)
        assertEquals(null, result2.downloadErrorDetails)
        assertEquals(null, result2.downloadTaskId)
    }

    @Test
    fun doNotUpdateToNonIdleStatesWithDifferentWorkerId() = runTest {
        val episode = episode.copy(downloadTaskId = UUID.randomUUID().toString())
        episodeDao.update(episode)

        val updates = listOf(
            DownloadStatusUpdate.WaitingForWifi(workerId),
            DownloadStatusUpdate.WaitingForPower(workerId),
            DownloadStatusUpdate.WaitingForStorage(workerId),
            DownloadStatusUpdate.Enqueued(workerId),
            DownloadStatusUpdate.InProgress(workerId),
            DownloadStatusUpdate.Success(workerId, File("audio.mp3")),
            DownloadStatusUpdate.Failure(workerId, "error_message"),
        )

        for (update in updates) {
            episodeDao.updateDownloadStatuses(mapOf(episode.uuid to update))
            val result = episodeDao.findByUuid(episode.uuid)!!

            assertEquals(episode, result)
        }
    }

    @Test
    fun updateToIdleStatesWithoutWorkerId() = runTest {
        episodeDao.update(episode.copy(downloadTaskId = null))

        episodeDao.updateDownloadStatuses(mapOf(episode.uuid to DownloadStatusUpdate.Idle))
        val result = episodeDao.findByUuid(episode.uuid)!!

        assertEquals(EpisodeDownloadStatus.DownloadNotRequested, result.downloadStatus)
        assertEquals(null, result.downloadedFilePath)
        assertEquals(null, result.downloadErrorDetails)
        assertEquals(null, result.downloadTaskId)
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

        episodeDao.setReadyForDownload(mapOf(episode.uuid to workerId))
        val result = episodeDao.findByUuid(episode.uuid)!!

        assertEquals(false, result.isArchived)
        assertEquals(EpisodeDownloadStatus.Queued, result.downloadStatus)
        assertEquals(workerId.toString(), result.downloadTaskId)
        assertEquals(false, result.isExemptFromAutoDownload)
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

        episodeDao.setReadyForDownload(mapOf(episode.uuid to workerId))
        val result = episodeDao.findByUuid(episode.uuid)!!

        assertEquals(episode, result)
    }

    @Test
    fun setDownloadCancelledWithDisabledAutoDownloads() = runTest {
        val episode = episode.copy(
            downloadStatus = EpisodeDownloadStatus.Downloading,
            autoDownloadStatus = PodcastEpisode.AUTO_DOWNLOAD_STATUS_MANUALLY_DOWNLOADED,
        )
        episodeDao.update(episode)

        episodeDao.setDownloadCancelled(setOf(episode.uuid), disableAutoDownload = true)
        val result = episodeDao.findByUuid(episode.uuid)!!

        assertEquals(EpisodeDownloadStatus.DownloadNotRequested, result.downloadStatus)
        assertEquals(true, result.isExemptFromAutoDownload)
        assertEquals(null, result.downloadTaskId)
    }

    @Test
    fun setDownloadCancelledWithoutDisabledAutoDownloads() = runTest {
        val episode = episode.copy(
            downloadStatus = EpisodeDownloadStatus.Downloading,
            autoDownloadStatus = PodcastEpisode.AUTO_DOWNLOAD_STATUS_MANUALLY_DOWNLOADED,
        )
        episodeDao.update(episode)

        episodeDao.setDownloadCancelled(setOf(episode.uuid), disableAutoDownload = false)
        val result = episodeDao.findByUuid(episode.uuid)!!

        assertEquals(EpisodeDownloadStatus.DownloadNotRequested, result.downloadStatus)
        assertEquals(false, result.isExemptFromAutoDownload)
        assertEquals(null, result.downloadTaskId)
    }
}
