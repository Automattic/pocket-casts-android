package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.media3.common.MimeTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `defaultHlsStreamUrl returns the first hls when streaming is enabled`() {
        val enclosures = listOf(enclosure(MimeTypes.APPLICATION_M3U8, "https://example.com/master.m3u8"))
        assertEquals("https://example.com/master.m3u8", enclosures.defaultHlsStreamUrl(hlsStreamingEnabled = true))
    }

    @Test
    fun `defaultHlsStreamUrl returns null when streaming is disabled`() {
        val enclosures = listOf(enclosure(MimeTypes.APPLICATION_M3U8, "https://example.com/master.m3u8"))
        assertNull(enclosures.defaultHlsStreamUrl(hlsStreamingEnabled = false))
    }

    private fun enclosure(type: String, vararg uris: String) = EpisodeAlternateEnclosure(
        episodeUuid = "episode-uuid",
        position = 0,
        type = type,
        sources = uris.map { AlternateEnclosureSource(uri = it) },
    )
}
