package au.com.shiftyjelly.pocketcasts.repositories.download

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.WorkInfo
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeDownloadError
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import java.io.File
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadWorkInfoTest {
    private val episodeTag = DownloadEpisodeWorker.episodeTag("episode-id")
    private val podcastTag = DownloadEpisodeWorker.podcastTag("podcast-id")
    private val sourceViewTag = DownloadEpisodeWorker.sourceViewTag(SourceView.PLAYER)

    @Test
    fun `ignore work without episode tag`() {
        val workInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.ENQUEUED,
            tags = setOf("episode-id", podcastTag),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertNull(downloadInfo)
    }

    @Test
    fun `ignore work without podcast tag`() {
        val workInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.ENQUEUED,
            tags = setOf(episodeTag, "podcast-id"),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertNull(downloadInfo)
    }

    @Test
    fun `ignore work with partial episode tag`() {
        val workInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.ENQUEUED,
            tags = setOf(DownloadEpisodeWorker.WORKER_EPISODE_TAG_PREFIX, podcastTag),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertNull(downloadInfo)
    }

    @Test
    fun `ignore work with partial podcast tag`() {
        val workInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.ENQUEUED,
            tags = setOf(episodeTag, DownloadEpisodeWorker.WORKER_PODCAST_TAG_PREFIX),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertNull(downloadInfo)
    }

    @Test
    fun `map enqueued work info`() {
        val workInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.ENQUEUED,
            tags = setOf(episodeTag, podcastTag),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.Pending(
                id = workInfo.id,
                episodeUuid = "episode-id",
                podcastUuid = "podcast-id",
                runAttemptCount = 0,
                sourceView = SourceView.UNKNOWN,
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
            tags = setOf(episodeTag, podcastTag),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.Pending(
                id = workInfo.id,
                episodeUuid = "episode-id",
                podcastUuid = "podcast-id",
                runAttemptCount = 0,
                sourceView = SourceView.UNKNOWN,
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
            tags = setOf(episodeTag, podcastTag),
            progress = Data.Builder().putBoolean(IS_WORK_EXECUTING_KEY, true).build(),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.InProgress(
                id = workInfo.id,
                episodeUuid = "episode-id",
                podcastUuid = "podcast-id",
                runAttemptCount = 0,
                sourceView = SourceView.UNKNOWN,
            ),
            downloadInfo,
        )
    }

    @Test
    fun `map running work info with false progress data`() {
        val workInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.RUNNING,
            tags = setOf(episodeTag, podcastTag),
            progress = Data.Builder().putBoolean(IS_WORK_EXECUTING_KEY, false).build(),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.Pending(
                id = workInfo.id,
                episodeUuid = "episode-id",
                podcastUuid = "podcast-id",
                runAttemptCount = 0,
                sourceView = SourceView.UNKNOWN,
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
            tags = setOf(episodeTag, podcastTag),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.Pending(
                id = workInfo.id,
                episodeUuid = "episode-id",
                podcastUuid = "podcast-id",
                runAttemptCount = 0,
                sourceView = SourceView.UNKNOWN,
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
            tags = setOf(episodeTag, podcastTag),
            outputData = Data.Builder().putString(DOWNLOAD_FILE_PATH_KEY, "file.mp3").build(),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.Success(
                id = workInfo.id,
                episodeUuid = "episode-id",
                podcastUuid = "podcast-id",
                runAttemptCount = 0,
                sourceView = SourceView.UNKNOWN,
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
            tags = setOf(episodeTag, podcastTag),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.Failure(
                id = workInfo.id,
                episodeUuid = "episode-id",
                podcastUuid = "podcast-id",
                runAttemptCount = 0,
                sourceView = SourceView.UNKNOWN,
                error = EpisodeDownloadError(),
                errorMessage = MISSING_DOWNLOADED_FILE_PATH_ERROR,
            ),
            downloadInfo,
        )
    }

    @Test
    fun `map failed work info`() {
        val workInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.FAILED,
            tags = setOf(episodeTag, podcastTag),
            outputData = Data.Builder().putString(ERROR_MESSAGE_KEY, "Whoops!").build(),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.Failure(
                id = workInfo.id,
                episodeUuid = "episode-id",
                podcastUuid = "podcast-id",
                runAttemptCount = 0,
                sourceView = SourceView.UNKNOWN,
                error = EpisodeDownloadError(),
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
            tags = setOf(episodeTag, podcastTag),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.Failure(
                id = workInfo.id,
                episodeUuid = "episode-id",
                podcastUuid = "podcast-id",
                runAttemptCount = 0,
                sourceView = SourceView.UNKNOWN,
                error = EpisodeDownloadError(),
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
            tags = setOf(episodeTag, podcastTag),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.Cancelled(
                id = workInfo.id,
                episodeUuid = "episode-id",
                podcastUuid = "podcast-id",
                runAttemptCount = 0,
                sourceView = SourceView.UNKNOWN,
            ),
            downloadInfo,
        )
    }

    @Test
    fun `map work info with wifi constraint`() {
        val workInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.ENQUEUED,
            constraints = Constraints(requiredNetworkType = NetworkType.UNMETERED),
            tags = setOf(episodeTag, podcastTag),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.Pending(
                id = workInfo.id,
                episodeUuid = "episode-id",
                podcastUuid = "podcast-id",
                runAttemptCount = 0,
                sourceView = SourceView.UNKNOWN,
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
            tags = setOf(episodeTag, podcastTag),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.Pending(
                id = workInfo.id,
                episodeUuid = "episode-id",
                podcastUuid = "podcast-id",
                runAttemptCount = 0,
                sourceView = SourceView.UNKNOWN,
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
            tags = setOf(episodeTag, podcastTag),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.Pending(
                id = workInfo.id,
                episodeUuid = "episode-id",
                podcastUuid = "podcast-id",
                runAttemptCount = 0,
                sourceView = SourceView.UNKNOWN,
                isWifiRequired = false,
                isPowerRequired = false,
                isStorageRequired = true,
            ),
            downloadInfo,
        )
    }

    @Test
    fun `map work info's run attempt count`() {
        val workInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.ENQUEUED,
            runAttemptCount = 10,
            tags = setOf(episodeTag, podcastTag),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.Pending(
                id = workInfo.id,
                episodeUuid = "episode-id",
                podcastUuid = "podcast-id",
                runAttemptCount = 10,
                sourceView = SourceView.UNKNOWN,
                isWifiRequired = false,
                isPowerRequired = false,
                isStorageRequired = false,
            ),
            downloadInfo,
        )
    }

    @Test
    fun `map work info's source view`() {
        val workInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.ENQUEUED,
            tags = setOf(episodeTag, podcastTag, sourceViewTag),
        )

        val downloadInfo = DownloadEpisodeWorker.mapToDownloadWorkInfo(workInfo)

        assertEquals(
            DownloadWorkInfo.Pending(
                id = workInfo.id,
                episodeUuid = "episode-id",
                podcastUuid = "podcast-id",
                runAttemptCount = 0,
                sourceView = SourceView.PLAYER,
                isWifiRequired = false,
                isPowerRequired = false,
                isStorageRequired = false,
            ),
            downloadInfo,
        )
    }
}
