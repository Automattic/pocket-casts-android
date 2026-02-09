package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.UserEpisodeDao
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.DownloadStatusUpdate
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import com.squareup.moshi.Moshi
import java.io.File
import java.util.Date
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

    private val episode = UserEpisode(
        uuid = "episode-id",
        publishedDate = Date(),
        // Prepare episode with some data to verify correct state in tests
        episodeStatus = EpisodeStatusEnum.NOT_DOWNLOADED,
        downloadedFilePath = "invalid_path",
        downloadErrorDetails = "invalid_details",
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
    fun updateDownloadStatusToIdle() = runTest {
        userEpisodeDao.updateEpisodeStatusBlocking(episode.uuid, EpisodeStatusEnum.DOWNLOADING)
        userEpisodeDao.updateDownloadStatuses(mapOf(episode.uuid to DownloadStatusUpdate.Idle))

        val result = userEpisodeDao.findEpisodeByUuid(episode.uuid)!!

        assertEquals(EpisodeStatusEnum.NOT_DOWNLOADED, result.episodeStatus)
        assertEquals(null, result.downloadedFilePath)
        assertEquals(null, result.downloadErrorDetails)
    }

    @Test
    fun updateDownloadStatusToWaitingForWifi() = runTest {
        userEpisodeDao.updateDownloadStatuses(mapOf(episode.uuid to DownloadStatusUpdate.WaitingForWifi))

        val result = userEpisodeDao.findEpisodeByUuid(episode.uuid)!!

        assertEquals(EpisodeStatusEnum.WAITING_FOR_WIFI, result.episodeStatus)
        assertEquals(null, result.downloadedFilePath)
        assertEquals(null, result.downloadErrorDetails)
    }

    @Test
    fun updateDownloadStatusToWaitingForPower() = runTest {
        userEpisodeDao.updateDownloadStatuses(mapOf(episode.uuid to DownloadStatusUpdate.WaitingForPower))

        val result = userEpisodeDao.findEpisodeByUuid(episode.uuid)!!

        assertEquals(EpisodeStatusEnum.WAITING_FOR_POWER, result.episodeStatus)
        assertEquals(null, result.downloadedFilePath)
        assertEquals(null, result.downloadErrorDetails)
    }

    @Test
    fun updateDownloadStatusToEnqueued() = runTest {
        userEpisodeDao.updateDownloadStatuses(mapOf(episode.uuid to DownloadStatusUpdate.Enqueued))

        val result = userEpisodeDao.findEpisodeByUuid(episode.uuid)!!

        assertEquals(EpisodeStatusEnum.QUEUED, result.episodeStatus)
        assertEquals(null, result.downloadedFilePath)
        assertEquals(null, result.downloadErrorDetails)
    }

    @Test
    fun updateDownloadStatusToInProgress() = runTest {
        userEpisodeDao.updateDownloadStatuses(mapOf(episode.uuid to DownloadStatusUpdate.InProgress))

        val result = userEpisodeDao.findEpisodeByUuid(episode.uuid)!!

        assertEquals(EpisodeStatusEnum.DOWNLOADING, result.episodeStatus)
        assertEquals(null, result.downloadedFilePath)
        assertEquals(null, result.downloadErrorDetails)
    }

    @Test
    fun updateDownloadStatusToSuccess() = runTest {
        val file = File("podcast.mp3")
        userEpisodeDao.updateDownloadStatuses(mapOf(episode.uuid to DownloadStatusUpdate.Success(file)))

        val result = userEpisodeDao.findEpisodeByUuid(episode.uuid)!!

        assertEquals(EpisodeStatusEnum.DOWNLOADED, result.episodeStatus)
        assertEquals(file.path, result.downloadedFilePath)
        assertEquals(null, result.downloadErrorDetails)
    }

    @Test
    fun updateDownloadStatusToFailure() = runTest {
        val errorMessage = "Download failed"
        userEpisodeDao.updateDownloadStatuses(mapOf(episode.uuid to DownloadStatusUpdate.Failure(errorMessage)))

        val result = userEpisodeDao.findEpisodeByUuid(episode.uuid)!!

        assertEquals(EpisodeStatusEnum.DOWNLOAD_FAILED, result.episodeStatus)
        assertEquals(null, result.downloadedFilePath)
        assertEquals(errorMessage, result.downloadErrorDetails)
    }
}
