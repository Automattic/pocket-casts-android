package au.com.shiftyjelly.pocketcasts.models.entity

import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseEpisodeStreamOverrideTest {

    @Test
    fun `override stream url wins over download url`() {
        val episode = createEpisode(
            downloadUrl = "https://example.com/episode.mp3",
        ).apply {
            overrideStreamUrl = "https://example.com/video-1080.mp4"
            overrideStreamContentType = "video/mp4"
        }
        assertEquals("https://example.com/video-1080.mp4", episode.streamUrl)
    }

    @Test
    fun `stream url defaults to the progressive download when no override is set`() {
        val episode = createEpisode(
            downloadUrl = "https://example.com/episode.mp3",
        )
        assertEquals("https://example.com/episode.mp3", episode.streamUrl)
    }

    @Test
    fun `isStreamUrlHls reflects override content type`() {
        val hls = createEpisode(downloadUrl = "https://example.com/episode.mp3").apply {
            overrideStreamUrl = "https://example.com/master.m3u8"
            overrideStreamContentType = "application/x-mpegURL"
        }
        assertTrue(hls.isStreamUrlHls)

        val mp4 = createEpisode(downloadUrl = "https://example.com/episode.mp3").apply {
            overrideStreamUrl = "https://example.com/video-1080.mp4"
            overrideStreamContentType = "video/mp4"
        }
        assertFalse(mp4.isStreamUrlHls)
    }

    private fun createEpisode(
        downloadUrl: String? = null,
        fileType: String? = null,
    ): PodcastEpisode = PodcastEpisode(
        uuid = "test-uuid",
        publishedDate = Date(),
        downloadUrl = downloadUrl,
        fileType = fileType,
        downloadStatus = EpisodeDownloadStatus.DownloadNotRequested,
        playingStatus = EpisodePlayingStatus.NOT_PLAYED,
    )
}
