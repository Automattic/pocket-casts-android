package au.com.shiftyjelly.pocketcasts.servers.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiscoverEpisodeTest {

    @Test
    fun `videoUrl uses the default url when the file type is mp4`() {
        val episode = episode(url = "https://example.com/episode.mp4", fileType = "video/mp4")

        assertEquals("https://example.com/episode.mp4", episode.videoUrl)
    }

    @Test
    fun `videoUrl uses the default url when the file type is hls`() {
        val episode = episode(url = "https://example.com/episode.m3u8", fileType = "application/vnd.apple.mpegurl")

        assertEquals("https://example.com/episode.m3u8", episode.videoUrl)
    }

    @Test
    fun `videoUrl uses the first video alternate enclosure when the default is not video`() {
        val episode = episode(
            url = "https://example.com/episode.mp3",
            fileType = "audio/mpeg",
            alternateEnclosures = listOf(
                DiscoverAlternateEnclosure("audio/mpeg", listOf(DiscoverEnclosureSource("https://example.com/audio.mp3"))),
                DiscoverAlternateEnclosure("video/mp4", listOf(DiscoverEnclosureSource("https://example.com/video.mp4"))),
            ),
        )

        assertEquals("https://example.com/video.mp4", episode.videoUrl)
    }

    @Test
    fun `videoUrl is null when the default is audio and there are no alternate enclosures`() {
        val episode = episode(url = "https://example.com/episode.mp3", fileType = "audio/mpeg")

        assertNull(episode.videoUrl)
    }

    @Test
    fun `videoUrl is null when there is no url and no video enclosure`() {
        val episode = episode(
            url = null,
            fileType = "audio/mpeg",
            alternateEnclosures = listOf(
                DiscoverAlternateEnclosure("audio/mpeg", listOf(DiscoverEnclosureSource("https://example.com/audio.mp3"))),
            ),
        )

        assertNull(episode.videoUrl)
    }

    private fun episode(
        url: String?,
        fileType: String?,
        alternateEnclosures: List<DiscoverAlternateEnclosure>? = null,
    ) = DiscoverEpisode(
        uuid = "episode-uuid",
        title = "Episode",
        url = url,
        published = null,
        duration = null,
        fileType = fileType,
        size = null,
        podcast_uuid = "podcast-uuid",
        podcast_title = "Podcast",
        type = null,
        season = null,
        number = null,
        alternateEnclosures = alternateEnclosures,
    )
}
