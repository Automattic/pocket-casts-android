package au.com.shiftyjelly.pocketcasts.servers.podcast

import androidx.media3.common.MimeTypes
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpisodeInfoTest {
    private val adapter = Moshi.Builder().build().adapter(EpisodeInfo::class.java)

    @Test
    fun `parse episode with hls alternate enclosure`() {
        val episodeInfo = adapter.fromJson(
            """
            {
              "uuid": "episode-uuid",
              "url": "https://example.com/episode.mp3",
              "published": "2026-06-11T00:00:00Z",
              "alternate_enclosures": [
                { "type": "application/x-mpegURL", "length": 0, "sources": [{ "uri": "https://example.com/master.m3u8" }] }
              ]
            }
            """.trimIndent(),
        )

        val episode = episodeInfo?.toEpisode("podcast-uuid")
        assertEquals("https://example.com/episode.mp3", episode?.downloadUrl)
        assertEquals("https://example.com/master.m3u8", episode?.hlsUrl)
    }

    @Test
    fun `pick hls enclosure and ignore progressive mp4 alternates`() {
        val episodeInfo = adapter.fromJson(
            """
            {
              "uuid": "episode-uuid",
              "url": "https://example.com/episode.mp3",
              "published": "2026-06-11T00:00:00Z",
              "alternate_enclosures": [
                {
                  "type": "video/mp4",
                  "bitrate": 681484,
                  "height": 1080,
                  "default": true,
                  "sources": [{ "uri": "https://example.com/file-1080.mp4" }]
                },
                { "type": "application/x-mpegURL", "length": 0, "sources": [{ "uri": "https://example.com/master.m3u8" }] }
              ]
            }
            """.trimIndent(),
        )

        assertEquals("https://example.com/master.m3u8", episodeInfo?.toEpisode("podcast-uuid")?.hlsUrl)
    }

    @Test
    fun `captures full alternate enclosure metadata`() {
        // Mirrors the exact payload shape the backend emits.
        val episodeInfo = adapter.fromJson(
            """
            {
              "uuid": "episode-uuid",
              "url": "https://example.com/episode.mp3",
              "published": "2026-06-11T00:00:00Z",
              "alternate_enclosures": [
                {
                  "type": "application/x-mpegURL",
                  "length": 0,
                  "sources": [{ "uri": "https://example.com/master.m3u8" }]
                },
                {
                  "type": "video/mp4",
                  "length": 10562995,
                  "bitrate": 681484,
                  "height": 1080,
                  "title": "1080p",
                  "codecs": "avc1.640028,mp4a.40.2",
                  "default": true,
                  "sources": [
                    { "uri": "https://example.com/file-1080.mp4" },
                    { "uri": "ipfs://Qm..." },
                    { "uri": "https://example.com/file-1080.torrent", "content_type": "application/x-bittorrent" }
                  ],
                  "integrity": {
                    "type": "sri",
                    "value": "sha384-ExVqijgYHm15PqQqdXfW95x+Rs6C+d6E/ICxyQOeFevnxNLR/wtJNrNYTjIysUBo"
                  }
                }
              ]
            }
            """.trimIndent(),
        )

        val episode = episodeInfo!!.toEpisode("podcast-uuid")
        // The HLS enclosure is selected for the streaming fast-path.
        assertEquals("https://example.com/master.m3u8", episode?.hlsUrl)

        val enclosures = episodeInfo.toAlternateEnclosures()
        assertEquals(2, enclosures.size)

        val hls = enclosures[0]
        assertEquals("episode-uuid", hls.episodeUuid)
        assertEquals(0, hls.position)
        assertEquals(MimeTypes.APPLICATION_M3U8, hls.type)
        assertEquals(0L, hls.length)
        assertEquals("https://example.com/master.m3u8", hls.sources.single().uri)

        val mp4 = enclosures[1]
        assertEquals(1, mp4.position)
        assertEquals("video/mp4", mp4.type)
        assertEquals(681484L, mp4.bitrate)
        assertEquals(10562995L, mp4.length)
        assertEquals(1080, mp4.height)
        assertNull(mp4.width)
        assertEquals("1080p", mp4.title)
        assertEquals("avc1.640028,mp4a.40.2", mp4.codecs)
        assertEquals(true, mp4.isDefault)
        assertEquals("sri", mp4.integrityType)
        assertEquals("sha384-ExVqijgYHm15PqQqdXfW95x+Rs6C+d6E/ICxyQOeFevnxNLR/wtJNrNYTjIysUBo", mp4.integrityValue)
        assertEquals(3, mp4.sources.size)
        assertEquals("https://example.com/file-1080.mp4", mp4.sources[0].uri)
        assertNull(mp4.sources[0].contentType)
        assertEquals("ipfs://Qm...", mp4.sources[1].uri)
        assertEquals("application/x-bittorrent", mp4.sources[2].contentType)
    }

    @Test
    fun `parse episode without alternate enclosures`() {
        val episodeInfo = adapter.fromJson(
            """{"uuid":"episode-uuid","url":"https://example.com/episode.mp3","published":"2026-06-11T00:00:00Z"}""",
        )

        assertNull(episodeInfo?.alternateEnclosures)
        assertNull(episodeInfo?.toEpisode("podcast-uuid")?.hlsUrl)
    }

    @Test
    fun `no hls url when enclosures have no hls entry`() {
        val episodeInfo = adapter.fromJson(
            """
            {
              "uuid": "episode-uuid",
              "url": "https://example.com/episode.mp3",
              "published": "2026-06-11T00:00:00Z",
              "alternate_enclosures": [
                { "type": "video/mp4", "sources": [{ "uri": "https://example.com/file-1080.mp4" }] }
              ]
            }
            """.trimIndent(),
        )

        assertNull(episodeInfo?.toEpisode("podcast-uuid")?.hlsUrl)
    }
}
