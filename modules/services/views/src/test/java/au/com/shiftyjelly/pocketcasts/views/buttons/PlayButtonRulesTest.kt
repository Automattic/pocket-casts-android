package au.com.shiftyjelly.pocketcasts.views.buttons

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.views.buttons.PlayButtonMenuItem.Download
import au.com.shiftyjelly.pocketcasts.views.buttons.PlayButtonMenuItem.PlayLast
import au.com.shiftyjelly.pocketcasts.views.buttons.PlayButtonMenuItem.PlayNext
import au.com.shiftyjelly.pocketcasts.views.buttons.PlayButtonMenuItem.StopDownloading
import au.com.shiftyjelly.pocketcasts.views.buttons.PlayButtonMenuItem.Stream
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayButtonRulesTest {
    private fun episode(
        downloadStatus: EpisodeDownloadStatus = EpisodeDownloadStatus.DownloadNotRequested,
        playingStatus: EpisodePlayingStatus = EpisodePlayingStatus.NOT_PLAYED,
        playing: Boolean = false,
        playErrorDetails: String? = null,
    ) = PodcastEpisode(
        uuid = "uuid",
        publishedDate = Date(),
        downloadStatus = downloadStatus,
        playingStatus = playingStatus,
        playErrorDetails = playErrorDetails,
    ).also { it.playing = playing }

    @Test
    fun `playback failure wins over every other state`() {
        val episode = episode(EpisodeDownloadStatus.Downloaded, EpisodePlayingStatus.COMPLETED, playing = true, playErrorDetails = "Error")

        assertEquals(PlayButtonType.PLAYBACK_FAILED, PlayButtonType.from(episode, streamByDefault = false))
    }

    @Test
    fun `empty playback error is ignored`() {
        assertEquals(PlayButtonType.PAUSE, PlayButtonType.from(episode(playing = true, playErrorDetails = ""), streamByDefault = true))
    }

    @Test
    fun `playing wins over finished and downloaded`() {
        val episode = episode(EpisodeDownloadStatus.Downloaded, EpisodePlayingStatus.COMPLETED, playing = true)

        assertEquals(PlayButtonType.PAUSE, PlayButtonType.from(episode, streamByDefault = false))
    }

    @Test
    fun `finished wins over downloaded`() {
        val episode = episode(EpisodeDownloadStatus.Downloaded, EpisodePlayingStatus.COMPLETED)

        assertEquals(PlayButtonType.PLAYED, PlayButtonType.from(episode, streamByDefault = false))
    }

    @Test
    fun `downloaded plays regardless of streaming`() {
        assertEquals(PlayButtonType.PLAY, PlayButtonType.from(episode(EpisodeDownloadStatus.Downloaded), streamByDefault = false))
        assertEquals(PlayButtonType.PLAY, PlayButtonType.from(episode(EpisodeDownloadStatus.Downloaded), streamByDefault = true))
    }

    @Test
    fun `downloading and pending stop the download unless streaming`() {
        listOf(
            EpisodeDownloadStatus.Downloading,
            EpisodeDownloadStatus.Queued,
            EpisodeDownloadStatus.WaitingForWifi,
            EpisodeDownloadStatus.WaitingForPower,
            EpisodeDownloadStatus.WaitingForStorage,
        ).forEach { status ->
            assertEquals(PlayButtonType.STOP_DOWNLOAD, PlayButtonType.from(episode(status), streamByDefault = false))
            assertEquals(PlayButtonType.PLAY, PlayButtonType.from(episode(status), streamByDefault = true))
        }
    }

    @Test
    fun `not downloaded offers download unless streaming`() {
        listOf(EpisodeDownloadStatus.DownloadNotRequested, EpisodeDownloadStatus.DownloadFailed).forEach { status ->
            assertEquals(PlayButtonType.DOWNLOAD, PlayButtonType.from(episode(status), streamByDefault = false))
            assertEquals(PlayButtonType.PLAY, PlayButtonType.from(episode(status), streamByDefault = true))
        }
    }

    @Test
    fun `menu offers download when not requested and button is not download`() {
        assertEquals(
            listOf(PlayNext, PlayLast, Download),
            PlayButtonMenuItem.visibleItems(EpisodeDownloadStatus.DownloadNotRequested, PlayButtonType.PLAY),
        )
    }

    @Test
    fun `menu offers stream when not requested and button is download`() {
        assertEquals(
            listOf(PlayNext, PlayLast, Stream),
            PlayButtonMenuItem.visibleItems(EpisodeDownloadStatus.DownloadNotRequested, PlayButtonType.DOWNLOAD),
        )
    }

    @Test
    fun `menu offers stop downloading for cancellable statuses`() {
        listOf(
            EpisodeDownloadStatus.Queued,
            EpisodeDownloadStatus.WaitingForWifi,
            EpisodeDownloadStatus.WaitingForPower,
            EpisodeDownloadStatus.WaitingForStorage,
            EpisodeDownloadStatus.Downloading,
        ).forEach { status ->
            assertEquals(listOf(PlayNext, PlayLast, StopDownloading), PlayButtonMenuItem.visibleItems(status, PlayButtonType.STOP_DOWNLOAD))
        }
    }

    @Test
    fun `menu offers only queue actions for finished downloads`() {
        listOf(EpisodeDownloadStatus.Downloaded, EpisodeDownloadStatus.DownloadFailed).forEach { status ->
            assertEquals(listOf(PlayNext, PlayLast), PlayButtonMenuItem.visibleItems(status, PlayButtonType.PLAY))
        }
    }
}
