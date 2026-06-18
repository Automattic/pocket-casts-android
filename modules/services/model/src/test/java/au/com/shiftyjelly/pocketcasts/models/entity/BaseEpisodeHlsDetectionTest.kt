package au.com.shiftyjelly.pocketcasts.models.entity

import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BaseEpisodeHlsDetectionTest {

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    @Test
    fun `m3u8 extension is HLS only`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.m3u8")
        assertTrue(episode.isHlsOnly)
    }

    @Test
    fun `m3u8 with query params is HLS only`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.m3u8?token=abc&sig=123")
        assertTrue(episode.isHlsOnly)
    }

    @Test
    fun `m3u8 with fragment is HLS only`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.m3u8#fragment")
        assertTrue(episode.isHlsOnly)
    }

    @Test
    fun `m3u8 with query params and fragment is HLS only`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.m3u8?token=abc#frag")
        assertTrue(episode.isHlsOnly)
    }

    @Test
    fun `uppercase M3U8 extension is HLS only`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.M3U8")
        assertTrue(episode.isHlsOnly)
    }

    @Test
    fun `application x-mpegURL fileType is HLS only`() {
        val episode = createEpisode(downloadUrl = "https://example.com/stream", fileType = "application/x-mpegURL")
        assertTrue(episode.isHlsOnly)
    }

    @Test
    fun `application vnd apple mpegurl fileType is HLS only`() {
        val episode = createEpisode(downloadUrl = "https://example.com/stream", fileType = "application/vnd.apple.mpegurl")
        assertTrue(episode.isHlsOnly)
    }

    @Test
    fun `hls url without enclosure is HLS only`() {
        val episode = createEpisode(downloadUrl = null, hlsUrl = "https://example.com/episode.m3u8")
        assertTrue(episode.isHlsOnly)
    }

    @Test
    fun `mp3 URL is not HLS only`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.mp3")
        assertFalse(episode.isHlsOnly)
    }

    @Test
    fun `dual URL episode is not HLS only`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.mp3", hlsUrl = "https://example.com/episode.m3u8")
        assertFalse(episode.isHlsOnly)
    }

    @Test
    fun `null download URL and null fileType is not HLS only`() {
        val episode = createEpisode(downloadUrl = null, fileType = null)
        assertFalse(episode.isHlsOnly)
    }

    @Test
    fun `mp3 URL with audio mpeg fileType is not HLS only`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.mp3", fileType = "audio/mpeg")
        assertFalse(episode.isHlsOnly)
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

    @Test
    fun `HLS only episode cannot be queued for auto download`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.m3u8")
        assertFalse(episode.canQueueForAutoDownload)
    }

    @Test
    fun `non-HLS episode can be queued for auto download`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.mp3")
        assertTrue(episode.canQueueForAutoDownload)
    }

    @Test
    fun `dual URL episode can be queued for auto download`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.mp3", hlsUrl = "https://example.com/episode.m3u8")
        assertTrue(episode.canQueueForAutoDownload)
    }

    @Test
    fun `stream url prefers hls url when flag is enabled`() {
        FeatureFlag.setEnabled(Feature.HLS_STREAMING, true)
        val episode = createEpisode(downloadUrl = "https://example.com/episode.mp3", hlsUrl = "https://example.com/episode.m3u8")
        assertEquals("https://example.com/episode.m3u8", episode.streamUrl)
        assertTrue(episode.isStreamUrlHls)
    }

    @Test
    fun `stream url uses download url when flag is disabled`() {
        FeatureFlag.setEnabled(Feature.HLS_STREAMING, false)
        val episode = createEpisode(downloadUrl = "https://example.com/episode.mp3", hlsUrl = "https://example.com/episode.m3u8")
        assertEquals("https://example.com/episode.mp3", episode.streamUrl)
        assertFalse(episode.isStreamUrlHls)
    }

    @Test
    fun `stream url falls back to download url when hls url is null`() {
        FeatureFlag.setEnabled(Feature.HLS_STREAMING, true)
        val episode = createEpisode(downloadUrl = "https://example.com/episode.mp3")
        assertEquals("https://example.com/episode.mp3", episode.streamUrl)
        assertFalse(episode.isStreamUrlHls)
    }

    @Test
    fun `stream url is hls for m3u8 enclosure even when flag is disabled`() {
        FeatureFlag.setEnabled(Feature.HLS_STREAMING, false)
        val episode = createEpisode(downloadUrl = "https://example.com/episode.m3u8")
        assertEquals("https://example.com/episode.m3u8", episode.streamUrl)
        assertTrue(episode.isStreamUrlHls)
    }

    @Test
    fun `stream url is hls for HLS MIME type enclosure`() {
        FeatureFlag.setEnabled(Feature.HLS_STREAMING, false)
        val episode = createEpisode(downloadUrl = "https://example.com/stream", fileType = "application/x-mpegURL")
        assertTrue(episode.isStreamUrlHls)
    }

    private fun createEpisode(
        downloadUrl: String? = null,
        hlsUrl: String? = null,
        fileType: String? = null,
    ): PodcastEpisode = PodcastEpisode(
        uuid = "test-uuid",
        publishedDate = Date(),
        downloadUrl = downloadUrl,
        hlsUrl = hlsUrl,
        fileType = fileType,
        downloadStatus = EpisodeDownloadStatus.DownloadNotRequested,
        playingStatus = EpisodePlayingStatus.NOT_PLAYED,
    )
}
