package au.com.shiftyjelly.pocketcasts.wear.ui.episode

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadActionTest {
    @Test
    fun `ask before downloading on a metered connection when the data warning is on`() {
        val action = DownloadAction.from(episode(), warnOnMeteredNetwork = true, isUnmeteredConnection = false)

        assertEquals(DownloadAction.ConfirmDataUse, action)
    }

    @Test
    fun `wait for wifi on an unmetered connection when the data warning is on`() {
        val action = DownloadAction.from(episode(), warnOnMeteredNetwork = true, isUnmeteredConnection = true)

        assertEquals(DownloadAction.Download(waitForWifi = true), action)
    }

    @Test
    fun `download on any network when the data warning is off`() {
        assertEquals(
            DownloadAction.Download(waitForWifi = false),
            DownloadAction.from(episode(), warnOnMeteredNetwork = false, isUnmeteredConnection = false),
        )
        assertEquals(
            DownloadAction.Download(waitForWifi = false),
            DownloadAction.from(episode(), warnOnMeteredNetwork = false, isUnmeteredConnection = true),
        )
    }

    @Test
    fun `ask again before retrying a failed download on a metered connection`() {
        val action = DownloadAction.from(
            episode(EpisodeDownloadStatus.DownloadFailed),
            warnOnMeteredNetwork = true,
            isUnmeteredConnection = false,
        )

        assertEquals(DownloadAction.ConfirmDataUse, action)
    }

    @Test
    fun `cancel a pending download without asking`() {
        listOf(
            EpisodeDownloadStatus.Queued,
            EpisodeDownloadStatus.WaitingForWifi,
            EpisodeDownloadStatus.WaitingForPower,
            EpisodeDownloadStatus.WaitingForStorage,
            EpisodeDownloadStatus.Downloading,
        ).forEach { status ->
            val action = DownloadAction.from(episode(status), warnOnMeteredNetwork = true, isUnmeteredConnection = false)

            assertEquals("$status", DownloadAction.Cancel, action)
        }
    }

    @Test
    fun `do nothing for a downloaded episode`() {
        val action = DownloadAction.from(
            episode(EpisodeDownloadStatus.Downloaded),
            warnOnMeteredNetwork = true,
            isUnmeteredConnection = false,
        )

        assertEquals(DownloadAction.None, action)
    }

    private fun episode(status: EpisodeDownloadStatus = EpisodeDownloadStatus.DownloadNotRequested) = PodcastEpisode(
        uuid = "episode-uuid",
        publishedDate = Date(),
        downloadStatus = status,
    )
}
