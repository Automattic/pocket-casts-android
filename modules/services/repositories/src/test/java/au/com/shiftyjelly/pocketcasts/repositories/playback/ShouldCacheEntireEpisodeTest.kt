package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import java.util.Date
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShouldCacheEntireEpisodeTest {

    private fun createEpisode(
        downloadStatus: EpisodeDownloadStatus = EpisodeDownloadStatus.DownloadNotRequested,
        fileType: String? = "audio/mp3",
        sizeInBytes: Long = 0,
    ) = PodcastEpisode(
        uuid = "episode-uuid",
        publishedDate = Date(),
        downloadStatus = downloadStatus,
        fileType = fileType,
        sizeInBytes = sizeInBytes,
        downloadUrl = "https://example.com/episode.mp3",
    )

    private fun shouldCache(
        episode: PodcastEpisode = createEpisode(),
        isHlsStream: Boolean = false,
        cacheEntirePlayingEpisodeEnabled: Boolean = true,
        maxCacheSizeBytes: Long = CACHE_SIZE_BYTES,
    ) = shouldCacheEntireEpisode(
        episode = episode,
        isHlsStream = isHlsStream,
        cacheEntirePlayingEpisodeEnabled = cacheEntirePlayingEpisodeEnabled,
        maxCacheSizeBytes = maxCacheSizeBytes,
    )

    @Test
    fun `should cache a normal streamed audio episode`() {
        assertTrue(shouldCache())
    }

    @Test
    fun `should not cache a downloaded episode`() {
        assertFalse(shouldCache(createEpisode(downloadStatus = EpisodeDownloadStatus.Downloaded)))
    }

    @Test
    fun `should not cache a downloading episode`() {
        assertFalse(shouldCache(createEpisode(downloadStatus = EpisodeDownloadStatus.Downloading)))
    }

    @Test
    fun `should not cache an HLS stream`() {
        assertFalse(shouldCache(isHlsStream = true))
    }

    @Test
    fun `should not cache when the setting is disabled`() {
        assertFalse(shouldCache(cacheEntirePlayingEpisodeEnabled = false))
    }

    @Test
    fun `should not cache a video episode`() {
        assertFalse(shouldCache(createEpisode(fileType = "video/mp4")))
    }

    @Test
    fun `should not cache a small video episode`() {
        assertFalse(shouldCache(createEpisode(fileType = "video/mp4", sizeInBytes = 1L * 1024 * 1024)))
    }

    @Test
    fun `should not cache when the reported size exceeds the cache size`() {
        assertFalse(shouldCache(createEpisode(sizeInBytes = CACHE_SIZE_BYTES + 1)))
    }

    @Test
    fun `should cache when the reported size equals the cache size`() {
        assertTrue(shouldCache(createEpisode(sizeInBytes = CACHE_SIZE_BYTES)))
    }

    @Test
    fun `should cache when the reported size is unknown`() {
        assertTrue(shouldCache(createEpisode(sizeInBytes = 0)))
    }

    @Test
    fun `should cache a huge episode when there is no size cap`() {
        assertTrue(shouldCache(createEpisode(sizeInBytes = 48_089_985_249), maxCacheSizeBytes = 0))
    }

    private companion object {
        const val CACHE_SIZE_BYTES = 500L * 1024 * 1024
    }
}
