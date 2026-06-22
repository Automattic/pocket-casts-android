package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EpisodeLocationTest {

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    @Test
    fun `downloaded episode uses downloaded file path`() {
        val episode = createEpisode(
            downloadStatus = EpisodeDownloadStatus.Downloaded,
            downloadedFilePath = "/path/episode.mp3",
            hlsUrl = "https://example.com/episode.m3u8",
        )

        val location = EpisodeLocation.create(episode)

        assertTrue(location is EpisodeLocation.Downloaded)
        assertEquals("/path/episode.mp3", location.uri)
        assertFalse(location.isHlsStream)
    }

    @Test
    fun `stream prefers hls url when flag is enabled`() {
        FeatureFlag.setEnabled(Feature.HLS_STREAMING, true)
        val episode = createEpisode(hlsUrl = "https://example.com/episode.m3u8")

        val location = EpisodeLocation.create(episode)

        assertEquals("https://example.com/episode.m3u8", location.uri)
        assertTrue(location.isHlsStream)
    }

    @Test
    fun `stream uses download url when flag is disabled`() {
        FeatureFlag.setEnabled(Feature.HLS_STREAMING, false)
        val episode = createEpisode(hlsUrl = "https://example.com/episode.m3u8")

        val location = EpisodeLocation.create(episode)

        assertEquals("https://example.com/episode.mp3", location.uri)
        assertFalse(location.isHlsStream)
    }

    @Test
    fun `stream falls back to download url when hls url is null`() {
        FeatureFlag.setEnabled(Feature.HLS_STREAMING, true)
        val episode = createEpisode(hlsUrl = null)

        val location = EpisodeLocation.create(episode)

        assertEquals("https://example.com/episode.mp3", location.uri)
        assertFalse(location.isHlsStream)
    }

    private fun createEpisode(
        downloadStatus: EpisodeDownloadStatus = EpisodeDownloadStatus.DownloadNotRequested,
        downloadedFilePath: String? = null,
        hlsUrl: String? = null,
    ) = PodcastEpisode(
        uuid = "episode-uuid",
        publishedDate = Date(),
        downloadUrl = "https://example.com/episode.mp3",
        hlsUrl = hlsUrl,
        downloadedFilePath = downloadedFilePath,
        downloadStatus = downloadStatus,
    )
}
