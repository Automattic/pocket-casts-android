package au.com.shiftyjelly.pocketcasts.views.compose

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeRowData
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import au.com.shiftyjelly.pocketcasts.images.R as IR

@RunWith(RobolectricTestRunner::class)
class EpisodeRowStatusTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun episode(
        downloadStatus: EpisodeDownloadStatus = EpisodeDownloadStatus.DownloadNotRequested,
        playingStatus: EpisodePlayingStatus = EpisodePlayingStatus.NOT_PLAYED,
        isArchived: Boolean = false,
        isStarred: Boolean = false,
        playErrorDetails: String? = null,
    ) = PodcastEpisode(
        uuid = "uuid",
        title = "Episode title",
        publishedDate = Date(),
        duration = 3600.0,
        playedUpTo = if (playingStatus == EpisodePlayingStatus.IN_PROGRESS) 1200.0 else 0.0,
        playingStatus = playingStatus,
        downloadStatus = downloadStatus,
        isArchived = isArchived,
        isStarred = isStarred,
        playErrorDetails = playErrorDetails,
    )

    private fun rowData(
        downloadProgress: Int = 0,
        isBuffering: Boolean = false,
    ) = EpisodeRowData(
        downloadProgress = downloadProgress,
        playbackState = PlaybackState(episodeUuid = if (isBuffering) "uuid" else "", isBuffering = isBuffering),
        isInUpNext = false,
        hasBookmarks = false,
        hasHlsAlternateEnclosure = false,
    )

    private fun status(episode: PodcastEpisode, rowData: EpisodeRowData = rowData()) = EpisodeRowStatus.from(episode, rowData, context)

    private fun description(episode: PodcastEpisode, rowData: EpisodeRowData = rowData(), isInUpNext: Boolean = false) = episodeRowContentDescription(episode, "Today", status(episode, rowData), isInUpNext, context)

    @Test
    fun `shows time left without an icon by default`() {
        val status = status(episode())

        assertEquals(EpisodeRowStatus(text = "1h", description = "1 hour"), status)
    }

    @Test
    fun `shows downloaded icon with time left`() {
        val status = status(episode(EpisodeDownloadStatus.Downloaded, EpisodePlayingStatus.IN_PROGRESS))

        assertEquals(EpisodeRowStatus("40m left", "40 minutes left", IR.drawable.ic_downloaded, EpisodeStatusIconTint.Downloaded), status)
    }

    @Test
    fun `shows download progress`() {
        val status = status(episode(EpisodeDownloadStatus.Downloading), rowData(downloadProgress = 40))

        assertEquals(EpisodeRowStatus("Downloading… (40%)", progressCircle = ProgressCircleState.forPercent(0.4f)), status)
    }

    @Test
    fun `shows download queue states with their icons`() {
        listOf(
            EpisodeDownloadStatus.DownloadFailed to ("Download Failed" to IR.drawable.ic_download_failed_row),
            EpisodeDownloadStatus.WaitingForPower to ("Waiting for power" to IR.drawable.ic_waitingforpower),
            EpisodeDownloadStatus.WaitingForStorage to ("Waiting for storage space" to IR.drawable.ic_waitingforstorage),
            EpisodeDownloadStatus.WaitingForWifi to ("Waiting for WiFi" to IR.drawable.ic_waitingforwifi),
            EpisodeDownloadStatus.Queued to ("Queued for download" to IR.drawable.ic_waitingforwifi),
        ).forEach { (downloadStatus, expected) ->
            assertEquals(EpisodeRowStatus(expected.first, iconId = expected.second), status(episode(downloadStatus)))
        }
    }

    @Test
    fun `shows archived with time left`() {
        val status = status(episode(isArchived = true))

        assertEquals(EpisodeRowStatus("Archived · 1h", "Archived. 1 hour", IR.drawable.ic_archive), status)
    }

    @Test
    fun `download states take priority over archived and playback errors`() {
        val status = status(episode(EpisodeDownloadStatus.WaitingForWifi, isArchived = true, playErrorDetails = "Error"))

        assertEquals("Waiting for WiFi", status.text)
    }

    @Test
    fun `archived takes priority over playback errors`() {
        val status = status(episode(isArchived = true, playErrorDetails = "Error"))

        assertEquals(IR.drawable.ic_archive, status.iconId)
    }

    @Test
    fun `shows playback error details`() {
        val status = status(episode(playErrorDetails = "Unable to play"))

        assertEquals(EpisodeRowStatus("Unable to play", iconId = IR.drawable.ic_alert_small), status)
    }

    @Test
    fun `buffering replaces the text and icon but keeps the download progress circle`() {
        val status = status(episode(EpisodeDownloadStatus.Downloading), rowData(downloadProgress = 40, isBuffering = true))

        assertEquals("Buffering", status.text)
        assertEquals("", status.description)
        assertNull(status.iconId)
        assertTrue(status.isBuffering)
        assertEquals(ProgressCircleState.forPercent(0.4f), status.progressCircle)
    }

    @Test
    fun `buffering of another episode is ignored`() {
        val rowData = rowData().copy(playbackState = PlaybackState(episodeUuid = "other", isBuffering = true))

        assertEquals("1h", status(episode(), rowData).text)
    }

    @Test
    fun `describes a default row`() {
        assertEquals("Episode title. Today. 1 hour.", description(episode()))
    }

    @Test
    fun `describes in progress before up next`() {
        assertEquals(
            "Episode title. Today. 40 minutes left. In progress.",
            description(episode(playingStatus = EpisodePlayingStatus.IN_PROGRESS), isInUpNext = true),
        )
        assertEquals("Episode title. Today. 1 hour. In up next.", description(episode(), isInUpNext = true))
    }

    @Test
    fun `describes downloaded and starred rows`() {
        assertEquals(
            "Episode title. Today. 1 hour. Downloaded. Starred.",
            description(episode(EpisodeDownloadStatus.Downloaded, isStarred = true)),
        )
    }

    @Test
    fun `describes downloading rows with the status text`() {
        assertEquals(
            "Episode title. Today. Downloading… (40%). Downloading.",
            description(episode(EpisodeDownloadStatus.Downloading), rowData(downloadProgress = 40)),
        )
    }

    @Test
    fun `describes archived and buffering rows`() {
        assertEquals("Episode title. Today. Archived. 1 hour.", description(episode(isArchived = true)))
        assertEquals("Episode title. Today. Buffering.", description(episode(), rowData(isBuffering = true)))
    }
}
