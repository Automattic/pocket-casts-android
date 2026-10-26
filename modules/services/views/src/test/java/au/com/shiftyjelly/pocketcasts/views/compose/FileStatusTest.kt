package au.com.shiftyjelly.pocketcasts.views.compose

import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeRowData
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class FileStatusTest {
    private fun status(
        downloadStatus: EpisodeDownloadStatus = EpisodeDownloadStatus.DownloadNotRequested,
        serverStatus: UserEpisodeServerStatus = UserEpisodeServerStatus.LOCAL,
        playErrorDetails: String? = null,
        isBuffering: Boolean = false,
        hideErrorDetails: Boolean = false,
    ) = FileStatus.from(
        data = UserEpisodeRowData(
            episode = UserEpisode(
                uuid = "uuid",
                publishedDate = Date(),
                downloadStatus = downloadStatus,
                serverStatus = serverStatus,
                playErrorDetails = playErrorDetails,
            ),
            downloadProgress = 40,
            uploadProgress = 50,
            playbackState = PlaybackState(episodeUuid = "uuid", isBuffering = isBuffering),
            isInUpNext = false,
            hasBookmarks = false,
        ),
        hideErrorDetails = hideErrorDetails,
    )

    private fun text(id: Int, progress: Int? = null) = FileStatusText.Resource(id, progress)

    @Test
    fun `buffering wins over errors and downloads`() {
        val status = status(EpisodeDownloadStatus.Downloaded, playErrorDetails = "Error", isBuffering = true)

        assertEquals(FileStatus(text(LR.string.episode_row_buffering), isBuffering = true), status)
    }

    @Test
    fun `playback error shows untinted failed icon with details unless hidden`() {
        assertEquals(FileStatus(FileStatusText.Raw("Error"), FileStatusIcon.PlaybackFailed), status(playErrorDetails = "Error"))
        assertEquals(FileStatus(FileStatusText.Raw(""), FileStatusIcon.PlaybackFailed), status(playErrorDetails = "Error", hideErrorDetails = true))
    }

    @Test
    fun `download states map to their icon and text`() {
        val expected = mapOf(
            EpisodeDownloadStatus.Downloaded to FileStatus(FileStatusText.TimeLeft, FileStatusIcon.Downloaded),
            EpisodeDownloadStatus.Downloading to FileStatus(
                text(LR.string.episode_row_downloading, 40),
                progressCircle = ProgressCircleState.Progress(144f, clockwise = true),
            ),
            EpisodeDownloadStatus.DownloadFailed to FileStatus(text(LR.string.episode_row_download_failed), FileStatusIcon.DownloadFailed),
            EpisodeDownloadStatus.WaitingForPower to FileStatus(text(LR.string.episode_row_waiting_for_power), FileStatusIcon.WaitingForPower),
            EpisodeDownloadStatus.WaitingForStorage to FileStatus(text(LR.string.episode_row_waiting_for_storage), FileStatusIcon.WaitingForStorage),
            EpisodeDownloadStatus.WaitingForWifi to FileStatus(text(LR.string.episode_row_waiting_for_wifi), FileStatusIcon.WaitingForWifi),
            EpisodeDownloadStatus.Queued to FileStatus(text(LR.string.episode_row_queued)),
            EpisodeDownloadStatus.DownloadNotRequested to FileStatus(FileStatusText.TimeLeft),
        )

        expected.forEach { (downloadStatus, fileStatus) -> assertEquals(fileStatus, status(downloadStatus)) }
    }

    @Test
    fun `uploaded shows the cloud next to the download state`() {
        assertEquals(
            FileStatus(FileStatusText.TimeLeft, FileStatusIcon.Downloaded, isUploaded = true),
            status(EpisodeDownloadStatus.Downloaded, UserEpisodeServerStatus.UPLOADED),
        )
    }

    @Test
    fun `uploading overrides the download state but keeps buffering`() {
        assertEquals(
            FileStatus(
                text(LR.string.episode_row_uploading, 50),
                progressCircle = ProgressCircleState.Progress(180f, clockwise = true),
                isBuffering = true,
            ),
            status(EpisodeDownloadStatus.Downloaded, UserEpisodeServerStatus.UPLOADING, isBuffering = true),
        )
    }

    @Test
    fun `upload waiting for wifi and queued override the download state`() {
        assertEquals(
            FileStatus(text(LR.string.episode_row_waiting_for_wifi), FileStatusIcon.WaitingForWifi),
            status(EpisodeDownloadStatus.Downloading, UserEpisodeServerStatus.WAITING_FOR_WIFI, isBuffering = true),
        )
        assertEquals(
            FileStatus(text(LR.string.episode_row_queued_upload)),
            status(EpisodeDownloadStatus.Downloaded, UserEpisodeServerStatus.QUEUED),
        )
    }
}
