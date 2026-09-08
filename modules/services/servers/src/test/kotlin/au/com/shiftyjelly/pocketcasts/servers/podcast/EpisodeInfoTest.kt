package au.com.shiftyjelly.pocketcasts.servers.podcast

import androidx.media3.common.MimeTypes
import au.com.shiftyjelly.pocketcasts.models.entity.firstHlsStreamUrl
import au.com.shiftyjelly.pocketcasts.models.entity.firstProgressiveVideoStream
import au.com.shiftyjelly.pocketcasts.models.type.MediaKind
import au.com.shiftyjelly.pocketcasts.servers.di.NetworkModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpisodeInfoTest {
    private val adapter = NetworkModule().provideMoshi().adapter(EpisodeInfo::class.java)

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
        assertEquals("https://example.com/master.m3u8", episode?.alternateEnclosures?.firstHlsStreamUrl())
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

        assertEquals("https://example.com/master.m3u8", episodeInfo?.toEpisode("podcast-uuid")?.alternateEnclosures?.firstHlsStreamUrl())
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
                  "media_kind": "video",
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
        // The HLS enclosure is selectable for streaming.
        assertEquals("https://example.com/master.m3u8", episode?.alternateEnclosures?.firstHlsStreamUrl())

        val enclosures = episodeInfo.toAlternateEnclosures()
        assertEquals(2, enclosures.size)

        val hls = enclosures[0]
        assertEquals("episode-uuid", hls.episodeUuid)
        assertEquals(0, hls.position)
        assertEquals(MimeTypes.APPLICATION_M3U8, hls.type)
        assertNull(hls.mediaKind)
        assertEquals(0L, hls.length)
        assertEquals("https://example.com/master.m3u8", hls.sources.single().uri)

        val mp4 = enclosures[1]
        assertEquals(1, mp4.position)
        assertEquals("video/mp4", mp4.type)
        assertEquals(MediaKind.Video, mp4.mediaKind)
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
    fun `parse every known media kind`() {
        val episodeInfo = adapter.fromJson(
            """
            {
              "uuid": "episode-uuid",
              "url": "https://example.com/episode.mp3",
              "published": "2026-06-11T00:00:00Z",
              "alternate_enclosures": [
                { "type": "video/mp4", "media_kind": "video", "sources": [] },
                { "type": "audio/mpeg", "media_kind": "audio", "sources": [] },
                { "type": "video/mp4", "media_kind": "youtube", "sources": [] },
                { "type": "video/mp4", "media_kind": "vimeo", "sources": [] },
                { "type": "video/mp4", "media_kind": "other", "sources": [] }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(
            listOf(MediaKind.Video, MediaKind.Audio, MediaKind.YouTube, MediaKind.Vimeo, MediaKind.Other),
            episodeInfo?.toAlternateEnclosures()?.map { it.mediaKind },
        )
    }

    @Test
    fun `an unrecognised media kind keeps its raw value instead of failing`() {
        val episodeInfo = adapter.fromJson(
            """
            {
              "uuid": "episode-uuid",
              "url": "https://example.com/episode.mp3",
              "published": "2026-06-11T00:00:00Z",
              "alternate_enclosures": [
                { "type": "video/mp4", "media_kind": "hologram", "sources": [] },
                { "type": "video/mp4", "media_kind": null, "sources": [] },
                { "type": "video/mp4", "sources": [] },
                { "type": "video/mp4", "media_kind": 5, "sources": [] }
              ]
            }
            """.trimIndent(),
        )

        val enclosures = episodeInfo!!.toAlternateEnclosures()
        assertEquals(MediaKind.Unknown("hologram"), enclosures[0].mediaKind)
        // An explicit null and an absent key both mean "no media kind", which is not the same as MediaKind.Other.
        assertNull(enclosures[1].mediaKind)
        assertNull(enclosures[2].mediaKind)
        assertNull(enclosures[3].mediaKind)
    }

    @Test
    fun `hls-only episode without a progressive url is detected as hls-only`() {
        val episodeInfo = adapter.fromJson(
            """
            {
              "uuid": "episode-uuid",
              "url": "",
              "published": "2026-06-11T00:00:00Z",
              "alternate_enclosures": [
                { "type": "application/x-mpegURL", "length": 0, "sources": [{ "uri": "https://example.com/master.m3u8" }] }
              ]
            }
            """.trimIndent(),
        )

        val episode = episodeInfo?.toEpisode("podcast-uuid")
        assertEquals(MimeTypes.APPLICATION_M3U8, episode?.fileType)
        assertEquals(true, episode?.isHlsOnly)
    }

    @Test
    fun `video-only episode without a progressive url takes the alternate enclosure file type`() {
        val episodeInfo = adapter.fromJson(
            """
            {
              "uuid": "episode-uuid",
              "url": "",
              "published": "2026-06-11T00:00:00Z",
              "alternate_enclosures": [
                { "type": "video/mp4", "media_kind": "video", "sources": [{ "uri": "https://example.com/episode.mp4" }] }
              ]
            }
            """.trimIndent(),
        )

        val episode = episodeInfo?.toEpisode("podcast-uuid")
        assertEquals("video/mp4", episode?.fileType)
        assertEquals(true, episode?.isVideo)
        assertEquals(false, episode?.isHlsOnly)
    }

    @Test
    fun `episode with a progressive url keeps its own file type alongside a video alternate enclosure`() {
        val episodeInfo = adapter.fromJson(
            """
            {
              "uuid": "episode-uuid",
              "url": "https://example.com/episode.mp3",
              "file_type": "audio/mp3",
              "published": "2026-06-11T00:00:00Z",
              "alternate_enclosures": [
                { "type": "video/mp4", "media_kind": "video", "sources": [{ "uri": "https://example.com/episode.mp4" }] }
              ]
            }
            """.trimIndent(),
        )

        val episode = episodeInfo?.toEpisode("podcast-uuid")
        assertEquals("audio/mp3", episode?.fileType)
        assertEquals(
            "https://example.com/episode.mp4",
            episode?.alternateEnclosures?.firstProgressiveVideoStream()?.url,
        )
    }

    @Test
    fun `hls-only video episode keeps its video file type`() {
        val episodeInfo = adapter.fromJson(
            """
            {
              "uuid": "episode-uuid",
              "url": "",
              "file_type": "video/mp4",
              "published": "2026-06-11T00:00:00Z",
              "alternate_enclosures": [
                { "type": "application/x-mpegURL", "length": 0, "sources": [{ "uri": "https://example.com/master.m3u8" }] }
              ]
            }
            """.trimIndent(),
        )

        val episode = episodeInfo?.toEpisode("podcast-uuid")
        assertEquals("video/mp4", episode?.fileType)
        assertEquals(true, episode?.isVideo)
    }

    @Test
    fun `parse episode without alternate enclosures`() {
        val episodeInfo = adapter.fromJson(
            """{"uuid":"episode-uuid","url":"https://example.com/episode.mp3","published":"2026-06-11T00:00:00Z"}""",
        )

        assertNull(episodeInfo?.alternateEnclosures)
        assertNull(episodeInfo?.toEpisode("podcast-uuid")?.alternateEnclosures?.firstHlsStreamUrl())
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

        assertNull(episodeInfo?.toEpisode("podcast-uuid")?.alternateEnclosures?.firstHlsStreamUrl())
    }
}
