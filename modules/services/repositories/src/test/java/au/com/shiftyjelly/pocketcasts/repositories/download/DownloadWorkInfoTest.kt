package au.com.shiftyjelly.pocketcasts.repositories.download

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.WorkInfo
import java.io.File
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class DownloadWorkInfoTest {
    @Test
    fun `ignore work without episode tag`() {
        val workInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.ENQUEUED,
            tags = setOf("episode-id"),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertNull(downloadInfo)
    }

    @Test
    fun `ignore work with partial episode tag`() {
        val workInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.ENQUEUED,
            tags = setOf(DownloadEpisodeWorker.WORKER_EPISODE_TAG_PREFIX),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertNull(downloadInfo)
    }

    @Test
    fun `map enqueued work info`() {
        val workInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.ENQUEUED,
            tags = setOf(DownloadEpisodeWorker.episodeWorkerName("episode-id")),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.Pending(
                episodeUuid = "episode-id",
                isWifiRequired = false,
                isPowerRequired = false,
                isStorageRequired = false,
            ),
            downloadInfo,
        )
    }

    @Test
    fun `map blocked work info`() {
        val workInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.BLOCKED,
            tags = setOf(DownloadEpisodeWorker.episodeWorkerName("episode-id")),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.Pending(
                episodeUuid = "episode-id",
                isWifiRequired = false,
                isPowerRequired = false,
                isStorageRequired = false,
            ),
            downloadInfo,
        )
    }

    @Test
    fun `map running work info with true progress data`() {
        val workInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.RUNNING,
            tags = setOf(DownloadEpisodeWorker.episodeWorkerName("episode-id")),
            progress = Data.Builder().putBoolean(IS_WORK_EXECUTING, true).build(),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.InProgress(episodeUuid = "episode-id"),
            downloadInfo,
        )
    }

    @Test
    fun `map running work info with false progress data`() {
        val workInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.RUNNING,
            tags = setOf(DownloadEpisodeWorker.episodeWorkerName("episode-id")),
            progress = Data.Builder().putBoolean(IS_WORK_EXECUTING, false).build(),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.Pending(
                episodeUuid = "episode-id",
                isWifiRequired = false,
                isPowerRequired = false,
                isStorageRequired = false,
            ),
            downloadInfo,
        )
    }

    @Test
    fun `map running work info without running progress data`() {
        val workInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.RUNNING,
            tags = setOf(DownloadEpisodeWorker.episodeWorkerName("episode-id")),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.Pending(
                episodeUuid = "episode-id",
                isWifiRequired = false,
                isPowerRequired = false,
                isStorageRequired = false,
            ),
            downloadInfo,
        )
    }

    @Test
    fun `map succeeded work info`() {
        val workInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.SUCCEEDED,
            tags = setOf(DownloadEpisodeWorker.episodeWorkerName("episode-id")),
            outputData = Data.Builder().putString(DOWNLOAD_FILE_PATH_KEY, "file.mp3").build(),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.Success(
                episodeUuid = "episode-id",
                downloadFile = File("file.mp3"),
            ),
            downloadInfo,
        )
    }

    @Test
    fun `fail to map succeeded work info without output data`() {
        val workInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.SUCCEEDED,
            tags = setOf(DownloadEpisodeWorker.episodeWorkerName("episode-id")),
        )

        assertThrows("Output file path is missing for the episode episode-id download", IllegalArgumentException::class.java) {
            DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)
        }
    }

    @Test
    fun `map failed work info`() {
        val workInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.FAILED,
            tags = setOf(DownloadEpisodeWorker.episodeWorkerName("episode-id")),
            outputData = Data.Builder().putString(ERROR_MESSAGE_KEY, "Whoops!").build(),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.Failure(
                episodeUuid = "episode-id",
                errorMessage = "Whoops!",
            ),
            downloadInfo,
        )
    }

    @Test
    fun `map failed work info without error message`() {
        val workInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.FAILED,
            tags = setOf(DownloadEpisodeWorker.episodeWorkerName("episode-id")),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.Failure(
                episodeUuid = "episode-id",
                errorMessage = null,
            ),
            downloadInfo,
        )
    }

    @Test
    fun `map cancelled work info`() {
        val workInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.CANCELLED,
            tags = setOf(DownloadEpisodeWorker.episodeWorkerName("episode-id")),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.Cancelled(episodeUuid = "episode-id"),
            downloadInfo,
        )
    }

    @Test
    fun `map work info with wifi constraint`() {
        val workInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.ENQUEUED,
            constraints = Constraints(requiredNetworkType = NetworkType.UNMETERED),
            tags = setOf(DownloadEpisodeWorker.episodeWorkerName("episode-id")),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.Pending(
                episodeUuid = "episode-id",
                isWifiRequired = true,
                isPowerRequired = false,
                isStorageRequired = false,
            ),
            downloadInfo,
        )
    }

    @Test
    fun `map work info with power constraint`() {
        val workInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.ENQUEUED,
            constraints = Constraints(requiresCharging = true),
            tags = setOf(DownloadEpisodeWorker.episodeWorkerName("episode-id")),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.Pending(
                episodeUuid = "episode-id",
                isWifiRequired = false,
                isPowerRequired = true,
                isStorageRequired = false,
            ),
            downloadInfo,
        )
    }

    @Test
    fun `map work info with storage constraint`() {
        val workInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.ENQUEUED,
            constraints = Constraints(requiresStorageNotLow = true),
            tags = setOf(DownloadEpisodeWorker.episodeWorkerName("episode-id")),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.Pending(
                episodeUuid = "episode-id",
                isWifiRequired = false,
                isPowerRequired = false,
                isStorageRequired = true,
            ),
            downloadInfo,
        )
    }
}
