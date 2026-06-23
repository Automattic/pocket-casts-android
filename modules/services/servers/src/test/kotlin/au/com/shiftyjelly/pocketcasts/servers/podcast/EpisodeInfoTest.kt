package au.com.shiftyjelly.pocketcasts.servers.podcast

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
                  "length": 123456,
                  "height": 1080,
                  "width": 1920,
                  "lang": "en",
                  "title": "1080p",
                  "codecs": "avc1.640028",
                  "default": true,
                  "sources": [{ "uri": "https://example.com/file-1080.mp4", "content_type": "video/mp4" }]
                }
              ]
            }
            """.trimIndent(),
        )

        val enclosure = episodeInfo!!.toAlternateEnclosures().single()
        assertEquals("episode-uuid", enclosure.episodeUuid)
        assertEquals(0, enclosure.position)
        assertEquals("video/mp4", enclosure.type)
        assertEquals(681484L, enclosure.bitrate)
        assertEquals(123456L, enclosure.length)
        assertEquals(1080, enclosure.height)
        assertEquals(1920, enclosure.width)
        assertEquals("en", enclosure.lang)
        assertEquals("1080p", enclosure.title)
        assertEquals("avc1.640028", enclosure.codecs)
        assertEquals(true, enclosure.isDefault)
        assertEquals("https://example.com/file-1080.mp4", enclosure.sources.single().uri)
        assertEquals("video/mp4", enclosure.sources.single().contentType)
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
