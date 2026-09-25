package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeLocationTest {

    @Test
    fun `downloaded episode uses downloaded file path`() {
        val episode = createEpisode(
            downloadStatus = EpisodeDownloadStatus.Downloaded,
            downloadedFilePath = "/path/episode.mp3",
        )

        val location = EpisodeLocation.create(episode)

        assertTrue(location is EpisodeLocation.Downloaded)
        assertEquals("/path/episode.mp3", location.uri)
        assertFalse(location.isHlsStream)
    }

    @Test
    fun `streams the progressive download by default`() {
        val episode = createEpisode()

        val location = EpisodeLocation.create(episode)

        assertEquals("https://example.com/episode.mp3", location.uri)
        assertFalse(location.isHlsStream)
    }

    @Test
    fun `streams a selected hls override`() {
        val episode = createEpisode().apply {
            overrideStreamUrl = "https://example.com/episode.m3u8"
            overrideStreamContentType = "application/x-mpegURL"
        }

        val location = EpisodeLocation.create(episode)

        assertEquals("https://example.com/episode.m3u8", location.uri)
        assertTrue(location.isHlsStream)
    }

    @Test
    fun `downloaded episode prefers hls override stream`() {
        val episode = createEpisode(
            downloadStatus = EpisodeDownloadStatus.Downloaded,
            downloadedFilePath = "/path/episode.mp3",
        ).apply {
            overrideStreamUrl = "https://example.com/episode.m3u8"
            overrideStreamContentType = "application/x-mpegURL"
        }

        val location = EpisodeLocation.create(episode, preferStream = true)

        assertTrue(location is EpisodeLocation.Stream)
        assertEquals("https://example.com/episode.m3u8", location.uri)
        assertTrue(location.isHlsStream)
    }

    @Test
    fun `downloaded episode without hls stays downloaded when preferring stream`() {
        val episode = createEpisode(
            downloadStatus = EpisodeDownloadStatus.Downloaded,
            downloadedFilePath = "/path/episode.mp3",
        )

        val location = EpisodeLocation.create(episode, preferStream = true)

        assertTrue(location is EpisodeLocation.Downloaded)
        assertEquals("/path/episode.mp3", location.uri)
        assertFalse(location.isHlsStream)
    }

    @Test
    fun `not downloaded episode streams when preferring stream`() {
        val episode = createEpisode()

        val location = EpisodeLocation.create(episode, preferStream = true)

        assertTrue(location is EpisodeLocation.Stream)
        assertEquals("https://example.com/episode.mp3", location.uri)
        assertFalse(location.isHlsStream)
    }

    private fun createEpisode(
        downloadStatus: EpisodeDownloadStatus = EpisodeDownloadStatus.DownloadNotRequested,
        downloadedFilePath: String? = null,
    ) = PodcastEpisode(
        uuid = "episode-uuid",
        publishedDate = Date(),
        downloadUrl = "https://example.com/episode.mp3",
        downloadedFilePath = downloadedFilePath,
        downloadStatus = downloadStatus,
    )
}
