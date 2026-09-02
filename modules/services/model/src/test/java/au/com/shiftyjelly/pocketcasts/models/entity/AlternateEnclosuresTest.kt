package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.media3.common.MimeTypes
import au.com.shiftyjelly.pocketcasts.models.type.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlternateEnclosuresTest {

    @Test
    fun `selects hls source`() {
        val enclosures = listOf(
            enclosure(MimeTypes.APPLICATION_M3U8, "https://example.com/master.m3u8"),
        )
        assertEquals("https://example.com/master.m3u8", enclosures.firstHlsStreamUrl())
    }

    @Test
    fun `matches hls mime type case insensitively`() {
        val enclosures = listOf(
            enclosure("APPLICATION/X-MPEGURL", "https://example.com/master.m3u8"),
        )
        assertEquals("https://example.com/master.m3u8", enclosures.firstHlsStreamUrl())
    }

    @Test
    fun `matches apple vendor hls mime type`() {
        val enclosures = listOf(
            enclosure("application/vnd.apple.mpegurl", "https://example.com/master.m3u8"),
        )
        assertEquals("https://example.com/master.m3u8", enclosures.firstHlsStreamUrl())
    }

    @Test
    fun `matches every documented hls mime type`() {
        val hlsTypes = listOf(
            "application/vnd.apple.mpegurl",
            "audio/mpegurl",
            "application/x-mpegurl",
            "application/mpegurl",
            "audio/x-mpegurl",
        )
        hlsTypes.forEach { type ->
            val enclosures = listOf(enclosure(type, "https://example.com/master.m3u8"))
            assertEquals(type, "https://example.com/master.m3u8", enclosures.firstHlsStreamUrl())
        }
    }

    @Test
    fun `ignores non-hls enclosures and keeps hls`() {
        val enclosures = listOf(
            enclosure("video/mp4", "https://example.com/file-1080.mp4"),
            enclosure(MimeTypes.APPLICATION_M3U8, "https://example.com/master.m3u8"),
        )
        assertEquals("https://example.com/master.m3u8", enclosures.firstHlsStreamUrl())
    }

    @Test
    fun `skips non-http hls sources`() {
        val enclosures = listOf(
            enclosure(MimeTypes.APPLICATION_M3U8, "ipfs://QmManifest", "https://example.com/master.m3u8"),
        )
        assertEquals("https://example.com/master.m3u8", enclosures.firstHlsStreamUrl())
    }

    @Test
    fun `returns null when hls enclosure has no playable source`() {
        val enclosures = listOf(
            enclosure(MimeTypes.APPLICATION_M3U8, "ipfs://QmManifest"),
        )
        assertNull(enclosures.firstHlsStreamUrl())
    }

    @Test
    fun `returns null when hls enclosure has empty sources`() {
        val enclosures = listOf(
            enclosure(MimeTypes.APPLICATION_M3U8),
        )
        assertNull(enclosures.firstHlsStreamUrl())
    }

    @Test
    fun `returns null when no hls enclosure present`() {
        val enclosures = listOf(
            enclosure("video/mp4", "https://example.com/file-1080.mp4"),
        )
        assertNull(enclosures.firstHlsStreamUrl())
    }

    @Test
    fun `returns null for empty or null list`() {
        assertNull(emptyList<EpisodeAlternateEnclosure>().firstHlsStreamUrl())
        assertNull(null.firstHlsStreamUrl())
    }

    @Test
    fun `selects the progressive video source marked as video media kind`() {
        val enclosures = listOf(
            videoEnclosure("video/mp4", "https://example.com/episode.mp4"),
        )

        val stream = enclosures.firstProgressiveVideoStream()

        assertEquals("https://example.com/episode.mp4", stream?.url)
        assertEquals("video/mp4", stream?.contentType)
        assertFalse(stream!!.isHls)
        assertTrue(stream.isVideo)
    }

    @Test
    fun `keeps the enclosure type when a source declares a generic content type`() {
        val enclosures = listOf(
            EpisodeAlternateEnclosure(
                episodeUuid = "episode-uuid",
                position = 0,
                type = "video/mp4",
                mediaKind = MediaKind.Video,
                sources = listOf(AlternateEnclosureSource(uri = "https://example.com/episode.mp4", contentType = "application/octet-stream")),
            ),
        )

        val stream = enclosures.firstProgressiveVideoStream()

        assertEquals("video/mp4", stream?.contentType)
        assertTrue(stream!!.isVideo)
    }

    @Test
    fun `ignores a video mime type the server did not mark as video media kind`() {
        val enclosures = listOf(
            enclosure("video/mp4", "https://example.com/episode.mp4"),
        )

        assertNull(enclosures.firstProgressiveVideoStream())
    }

    @Test
    fun `ignores a video enclosure whose media kind we cannot stream directly`() {
        val enclosures = listOf(
            EpisodeAlternateEnclosure(
                episodeUuid = "episode-uuid",
                position = 0,
                type = "video/mp4",
                mediaKind = MediaKind.YouTube,
                sources = listOf(AlternateEnclosureSource(uri = "https://example.com/watch")),
            ),
        )

        assertNull(enclosures.firstProgressiveVideoStream())
    }

    @Test
    fun `skips non-http progressive video sources`() {
        val enclosures = listOf(
            videoEnclosure("video/mp4", "ipfs://QmEpisode", "https://example.com/episode.mp4"),
        )

        assertEquals("https://example.com/episode.mp4", enclosures.firstProgressiveVideoStream()?.url)
    }

    @Test
    fun `hls and progressive video are resolved independently`() {
        val enclosures = listOf(
            videoEnclosure("video/mp4", "https://example.com/episode.mp4"),
            videoEnclosure(MimeTypes.APPLICATION_M3U8, "https://example.com/master.m3u8"),
        )

        assertEquals("https://example.com/master.m3u8", enclosures.firstHlsStream()?.url)
        assertEquals("https://example.com/episode.mp4", enclosures.firstProgressiveVideoStream()?.url)
    }

    @Test
    fun `hls enclosure marked as video is not also a progressive video rendition`() {
        val enclosures = listOf(
            videoEnclosure(MimeTypes.APPLICATION_M3U8, "https://example.com/master.m3u8"),
        )

        assertNull(enclosures.firstProgressiveVideoStream())
    }

    @Test
    fun `falls through to the next hls enclosure when the first has no playable source`() {
        val enclosures = listOf(
            enclosure(MimeTypes.APPLICATION_M3U8, "ipfs://QmManifest"),
            enclosure(MimeTypes.APPLICATION_M3U8, "https://example.com/master.m3u8"),
        )

        assertEquals("https://example.com/master.m3u8", enclosures.firstHlsStreamUrl())
    }

    @Test
    fun `stream only mime type prefers hls over progressive video`() {
        val enclosures = listOf(
            videoEnclosure("video/mp4", "https://example.com/episode.mp4"),
            enclosure(MimeTypes.APPLICATION_M3U8, "https://example.com/master.m3u8"),
        )

        assertEquals(MimeTypes.APPLICATION_M3U8, enclosures.firstStreamOnlyMimeType())
    }

    @Test
    fun `stream only mime type falls back to the progressive video type`() {
        val enclosures = listOf(
            videoEnclosure("video/mp4", "https://example.com/episode.mp4"),
        )

        assertEquals("video/mp4", enclosures.firstStreamOnlyMimeType())
    }

    @Test
    fun `stream only mime type is null without a video enclosure`() {
        assertNull(listOf(enclosure("audio/mp3", "https://example.com/episode.mp3")).firstStreamOnlyMimeType())
        assertNull(null.firstStreamOnlyMimeType())
    }

    private fun enclosure(type: String, vararg uris: String) = EpisodeAlternateEnclosure(
        episodeUuid = "episode-uuid",
        position = 0,
        type = type,
        sources = uris.map { AlternateEnclosureSource(uri = it) },
    )

    private fun videoEnclosure(type: String, vararg uris: String) = enclosure(type, *uris).copy(mediaKind = MediaKind.Video)
}
