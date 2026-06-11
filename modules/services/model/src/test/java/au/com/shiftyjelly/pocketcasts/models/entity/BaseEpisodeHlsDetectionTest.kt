package au.com.shiftyjelly.pocketcasts.models.entity

import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import java.util.Date
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseEpisodeHlsDetectionTest {

    @Test
    fun `m3u8 extension is detected as HLS`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.m3u8")
        assertTrue(episode.isHLS)
    }

    @Test
    fun `m3u8 with query params is detected as HLS`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.m3u8?token=abc&sig=123")
        assertTrue(episode.isHLS)
    }

    @Test
    fun `m3u8 with fragment is detected as HLS`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.m3u8#fragment")
        assertTrue(episode.isHLS)
    }

    @Test
    fun `m3u8 with query params and fragment is detected as HLS`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.m3u8?token=abc#frag")
        assertTrue(episode.isHLS)
    }

    @Test
    fun `uppercase M3U8 extension is detected as HLS`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.M3U8")
        assertTrue(episode.isHLS)
    }

    @Test
    fun `application x-mpegURL fileType is detected as HLS`() {
        val episode = createEpisode(downloadUrl = "https://example.com/stream", fileType = "application/x-mpegURL")
        assertTrue(episode.isHLS)
    }

    @Test
    fun `application vnd apple mpegurl fileType is detected as HLS`() {
        val episode = createEpisode(downloadUrl = "https://example.com/stream", fileType = "application/vnd.apple.mpegurl")
        assertTrue(episode.isHLS)
    }

    @Test
    fun `mp3 URL is not HLS`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.mp3")
        assertFalse(episode.isHLS)
    }

    @Test
    fun `null download URL and null fileType is not HLS`() {
        val episode = createEpisode(downloadUrl = null, fileType = null)
        assertFalse(episode.isHLS)
    }

    @Test
    fun `mp3 URL with audio mpeg fileType is not HLS`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.mp3", fileType = "audio/mpeg")
        assertFalse(episode.isHLS)
    }

    @Test
    fun `m3u8 file extension is returned for HLS MIME type`() {
        val episode = createEpisode(fileType = "application/x-mpegURL")
        assert(episode.getFileExtension() == ".m3u8")
    }

    @Test
    fun `m3u8 file extension is returned for Apple HLS MIME type`() {
        val episode = createEpisode(fileType = "application/vnd.apple.mpegurl")
        assert(episode.getFileExtension() == ".m3u8")
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
