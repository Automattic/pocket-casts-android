package au.com.shiftyjelly.pocketcasts.servers

import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeAlternateEnclosure
import au.com.shiftyjelly.pocketcasts.models.type.MediaKind
import au.com.shiftyjelly.pocketcasts.servers.di.NetworkModule
import au.com.shiftyjelly.pocketcasts.servers.podcast.EpisodeInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DataParserTest {
    private val episodeInfoAdapter = NetworkModule().provideMoshi().adapter(EpisodeInfo::class.java)

    @Test
    fun `parse every known media kind`() {
        val enclosures = parseRefreshEnclosures(
            """
            { "type": "video/mp4", "media_kind": "video", "sources": [] },
            { "type": "audio/mpeg", "media_kind": "audio", "sources": [] },
            { "type": "video/mp4", "media_kind": "youtube", "sources": [] },
            { "type": "video/mp4", "media_kind": "vimeo", "sources": [] },
            { "type": "video/mp4", "media_kind": "other", "sources": [] }
            """.trimIndent(),
        )

        assertEquals(
            listOf(MediaKind.Video, MediaKind.Audio, MediaKind.YouTube, MediaKind.Vimeo, MediaKind.Other),
            enclosures.map { it.mediaKind },
        )
    }

    @Test
    fun `an unrecognised media kind keeps its raw value instead of failing`() {
        val enclosures = parseRefreshEnclosures(
            """
            { "type": "video/mp4", "media_kind": "hologram", "sources": [] },
            { "type": "video/mp4", "media_kind": null, "sources": [] },
            { "type": "video/mp4", "sources": [] }
            """.trimIndent(),
        )

        assertEquals(MediaKind.Unknown("hologram"), enclosures[0].mediaKind)
        // An explicit null and an absent key both mean "no media kind", which is not the same as MediaKind.Other.
        assertNull(enclosures[1].mediaKind)
        assertNull(enclosures[2].mediaKind)
    }

    @Test
    fun `refresh and podcast responses parse enclosures identically`() {
        // The two paths parse the same feed data, so a kind stored on refresh must match one stored from a podcast load.
        val enclosuresJson = """
            { "type": "application/x-mpegURL", "length": 0, "sources": [{ "uri": "https://example.com/master.m3u8" }] },
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
                { "uri": "https://example.com/file-1080.torrent", "content_type": "application/x-bittorrent" }
              ],
              "integrity": { "type": "sri", "value": "sha384-abc" }
            },
            { "type": "video/mp4", "media_kind": "hologram", "sources": [] },
            { "type": "video/mp4", "sources": [] }
        """.trimIndent()

        val fromRefresh = parseRefreshEnclosures(enclosuresJson)
        val fromPodcastResponse = episodeInfoAdapter.fromJson(
            """
            {
              "uuid": "$EPISODE_UUID",
              "url": "https://example.com/episode.mp3",
              "published": "2026-06-11T00:00:00Z",
              "alternate_enclosures": [$enclosuresJson]
            }
            """.trimIndent(),
        )!!.toAlternateEnclosures()

        assertEquals(fromPodcastResponse, fromRefresh)
    }

    private fun parseRefreshEnclosures(enclosuresJson: String): List<EpisodeAlternateEnclosure> {
        val response = DataParser.parseRefreshPodcasts(
            """
            {
              "podcast_updates": {
                "$PODCAST_UUID": [
                  {
                    "uuid": "$EPISODE_UUID",
                    "url": "https://example.com/episode.mp3",
                    "published_at": "2026-06-11 00:00:00",
                    "alternate_enclosures": [$enclosuresJson]
                  }
                ]
              }
            }
            """.trimIndent(),
        )

        return response!!.getUpdatesForPodcast(PODCAST_UUID)!!.single().alternateEnclosures
    }

    companion object {
        private const val PODCAST_UUID = "podcast-uuid"
        private const val EPISODE_UUID = "episode-uuid"
    }
}
