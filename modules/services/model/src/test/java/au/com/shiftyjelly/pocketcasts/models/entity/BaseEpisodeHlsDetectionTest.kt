package au.com.shiftyjelly.pocketcasts.models.entity

import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.MediaKind
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseEpisodeHlsDetectionTest {

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
    fun `mp3 URL is not HLS only`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.mp3")
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
    fun `stream url defaults to the progressive download`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.mp3")
        assertEquals("https://example.com/episode.mp3", episode.streamUrl)
        assertFalse(episode.isStreamUrlHls)
    }

    @Test
    fun `stream url is hls for an m3u8 enclosure`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.m3u8")
        assertEquals("https://example.com/episode.m3u8", episode.streamUrl)
        assertTrue(episode.isStreamUrlHls)
    }

    @Test
    fun `stream url is hls for an HLS MIME type enclosure`() {
        val episode = createEpisode(downloadUrl = "https://example.com/stream", fileType = "application/x-mpegURL")
        assertTrue(episode.isStreamUrlHls)
    }

    @Test
    fun `video episode shows the video icon`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.mp4", fileType = "video/mp4")
        assertTrue(episode.showsVideoIcon(hasVideoAlternateEnclosure = false))
    }

    @Test
    fun `HLS only episode shows the video icon`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.m3u8")
        assertTrue(episode.showsVideoIcon(hasVideoAlternateEnclosure = false))
    }

    @Test
    fun `episode with an HLS alternate enclosure shows the video icon`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.mp3")
        assertTrue(episode.showsVideoIcon(hasVideoAlternateEnclosure = true))
    }

    @Test
    fun `plain audio episode does not show the video icon`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.mp3", fileType = "audio/mpeg")
        assertFalse(episode.showsVideoIcon(hasVideoAlternateEnclosure = false))
    }

    @Test
    fun `episode streaming a video alternate enclosure is a video stream but not a video file`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.mp3", fileType = "audio/mp3").apply {
            overrideStream = AlternateEnclosureStream(url = "https://example.com/episode.mp4", contentType = "video/mp4", mediaKind = MediaKind.Video)
        }

        assertTrue(episode.isStreamUrlVideo)
        assertFalse(episode.isStreamUrlHls)
        // The episode's own enclosure is still audio, so anything reading it from the database sees audio.
        assertFalse(episode.isVideo)
    }

    @Test
    fun `episode streaming an hls alternate enclosure is a video stream but not a video file`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.mp3", fileType = "audio/mp3").apply {
            overrideStream = AlternateEnclosureStream(url = "https://example.com/master.m3u8", contentType = "application/x-mpegURL", mediaKind = null)
        }

        assertFalse(episode.isVideo)
        assertTrue(episode.isStreamUrlVideo)
    }

    @Test
    fun `audio episode without a resolved stream is neither video nor a video stream`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.mp3", fileType = "audio/mp3")

        assertFalse(episode.isVideo)
        assertFalse(episode.isStreamUrlVideo)
    }

    @Test
    fun `video rendition is trusted from the media kind, not the content type the source declared`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.mp3", fileType = "audio/mp3").apply {
            overrideStream = AlternateEnclosureStream(url = "https://example.com/episode.mp4", contentType = "application/octet-stream", mediaKind = MediaKind.Video)
        }

        assertTrue(episode.isStreamUrlVideo)
    }

    @Test
    fun `video file episode is not treated as an alternate video stream`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.mp4", fileType = "video/mp4")

        assertTrue(episode.isVideo)
        assertFalse(episode.isStreamUrlVideo)
    }

    @Test
    fun `episode whose feed sent no progressive url has nothing to download`() {
        val episode = createEpisode(downloadUrl = "", fileType = "video/mp4")

        assertTrue(episode.isStreamOnly)
        assertFalse(episode.canQueueForAutoDownload)
        // Its own enclosure is video, so isHlsOnly cannot be what guards the download.
        assertFalse(episode.isHlsOnly)
    }

    @Test
    fun `episode awaiting a refreshed download url is still downloadable`() {
        val episode = createEpisode(downloadUrl = null, fileType = "audio/mp3")

        assertFalse(episode.isStreamOnly)
        assertTrue(episode.canQueueForAutoDownload)
    }

    @Test
    fun `hls only episode is still stream only`() {
        assertTrue(createEpisode(downloadUrl = "https://example.com/episode.m3u8").isStreamOnly)
    }

    @Test
    fun `episode with a progressive url is not stream only`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.mp3", fileType = "audio/mp3")

        assertFalse(episode.isStreamOnly)
        assertTrue(episode.canQueueForAutoDownload)
    }

    @Test
    fun `stream content type describes the resolved encoding, never the progressive enclosure`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.mp3", fileType = "audio/mpeg")
        assertEquals("audio/mpeg", episode.streamContentType)

        // A rendition the server sent no type for must not borrow the progressive enclosure's.
        episode.overrideStream = AlternateEnclosureStream(
            url = "https://example.com/episode.mp4",
            contentType = null,
            mediaKind = MediaKind.Video,
        )
        assertNull(episode.streamContentType)
    }

    @Test
    fun `an alternate video rendition must not share the progressive file's cache entry`() {
        val episode = createEpisode(downloadUrl = "https://example.com/episode.mp3", fileType = "audio/mp3")
        assertTrue(episode.usesSharedPlayerCache)

        episode.overrideStream = AlternateEnclosureStream(
            url = "https://example.com/episode.mp4",
            contentType = "video/mp4",
            mediaKind = MediaKind.Video,
        )
        assertFalse(episode.usesSharedPlayerCache)
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
