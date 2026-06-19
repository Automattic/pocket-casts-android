package au.com.shiftyjelly.pocketcasts.servers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlternateEnclosuresTest {

    @Test
    fun `selects hls source`() {
        val enclosures = listOf(
            AlternateEnclosureData("application/x-mpegURL", listOf("https://example.com/master.m3u8")),
        )
        assertEquals("https://example.com/master.m3u8", enclosures.firstHlsStreamUrl())
    }

    @Test
    fun `matches hls mime type case insensitively`() {
        val enclosures = listOf(
            AlternateEnclosureData("APPLICATION/X-MPEGURL", listOf("https://example.com/master.m3u8")),
        )
        assertEquals("https://example.com/master.m3u8", enclosures.firstHlsStreamUrl())
    }

    @Test
    fun `matches apple vendor hls mime type`() {
        val enclosures = listOf(
            AlternateEnclosureData("application/vnd.apple.mpegurl", listOf("https://example.com/master.m3u8")),
        )
        assertEquals("https://example.com/master.m3u8", enclosures.firstHlsStreamUrl())
    }

    @Test
    fun `ignores non-hls enclosures and keeps hls`() {
        val enclosures = listOf(
            AlternateEnclosureData("video/mp4", listOf("https://example.com/file-1080.mp4")),
            AlternateEnclosureData("application/x-mpegURL", listOf("https://example.com/master.m3u8")),
        )
        assertEquals("https://example.com/master.m3u8", enclosures.firstHlsStreamUrl())
    }

    @Test
    fun `skips non-http hls sources`() {
        val enclosures = listOf(
            AlternateEnclosureData(
                "application/x-mpegURL",
                listOf("ipfs://QmManifest", "https://example.com/master.m3u8"),
            ),
        )
        assertEquals("https://example.com/master.m3u8", enclosures.firstHlsStreamUrl())
    }

    @Test
    fun `returns null when hls enclosure has no playable source`() {
        val enclosures = listOf(
            AlternateEnclosureData("application/x-mpegURL", listOf("ipfs://QmManifest")),
        )
        assertNull(enclosures.firstHlsStreamUrl())
    }

    @Test
    fun `returns null when hls enclosure has empty sources`() {
        val enclosures = listOf(
            AlternateEnclosureData("application/x-mpegURL", emptyList()),
        )
        assertNull(enclosures.firstHlsStreamUrl())
    }

    @Test
    fun `returns null when no hls enclosure present`() {
        val enclosures = listOf(
            AlternateEnclosureData("video/mp4", listOf("https://example.com/file-1080.mp4")),
        )
        assertNull(enclosures.firstHlsStreamUrl())
    }

    @Test
    fun `returns null for empty or null list`() {
        assertNull(emptyList<AlternateEnclosureData>().firstHlsStreamUrl())
        assertNull(null.firstHlsStreamUrl())
    }
}
