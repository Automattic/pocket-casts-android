package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.work.NetworkType
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrefetchNextEpisodeTest {

    private fun createEpisode(
        uuid: String = "episode-uuid",
        downloadUrl: String? = "https://example.com/episode.mp3",
        downloadStatus: EpisodeDownloadStatus = EpisodeDownloadStatus.DownloadNotRequested,
    ) = PodcastEpisode(
        uuid = uuid,
        publishedDate = Date(),
        downloadUrl = downloadUrl,
        downloadStatus = downloadStatus,
    )

    @Test
    fun `should prefetch when all guards pass`() {
        val episode = createEpisode()
        val result = buildPrefetchRequest(
            isFeatureEnabled = true,
            isPlayerRemote = false,
            nextEpisode = episode,
            warnOnMeteredNetwork = false,
        )

        assertEquals("episode-uuid", result?.episodeUuid)
        assertEquals("https://example.com/episode.mp3", result?.downloadUrl)
        assertEquals(NetworkType.CONNECTED, result?.networkConstraint)
    }

    @Test
    fun `should not prefetch when feature flag is disabled`() {
        val result = buildPrefetchRequest(
            isFeatureEnabled = false,
            isPlayerRemote = false,
            nextEpisode = createEpisode(),
            warnOnMeteredNetwork = false,
        )

        assertNull(result)
    }

    @Test
    fun `should not prefetch when player is remote`() {
        val result = buildPrefetchRequest(
            isFeatureEnabled = true,
            isPlayerRemote = true,
            nextEpisode = createEpisode(),
            warnOnMeteredNetwork = false,
        )

        assertNull(result)
    }

    @Test
    fun `should not prefetch when queue is empty`() {
        val result = buildPrefetchRequest(
            isFeatureEnabled = true,
            isPlayerRemote = false,
            nextEpisode = null,
            warnOnMeteredNetwork = false,
        )

        assertNull(result)
    }

    @Test
    fun `should not prefetch when next episode is downloaded`() {
        val result = buildPrefetchRequest(
            isFeatureEnabled = true,
            isPlayerRemote = false,
            nextEpisode = createEpisode(downloadStatus = EpisodeDownloadStatus.Downloaded),
            warnOnMeteredNetwork = false,
        )

        assertNull(result)
    }

    @Test
    fun `should not prefetch when next episode is downloading`() {
        val result = buildPrefetchRequest(
            isFeatureEnabled = true,
            isPlayerRemote = false,
            nextEpisode = createEpisode(downloadStatus = EpisodeDownloadStatus.Downloading),
            warnOnMeteredNetwork = false,
        )

        assertNull(result)
    }

    @Test
    fun `should not prefetch when next episode is HLS`() {
        val result = buildPrefetchRequest(
            isFeatureEnabled = true,
            isPlayerRemote = false,
            nextEpisode = createEpisode(downloadUrl = "https://example.com/episode.m3u8"),
            warnOnMeteredNetwork = false,
        )

        assertNull(result)
    }

    @Test
    fun `should not prefetch when download URL is null`() {
        val result = buildPrefetchRequest(
            isFeatureEnabled = true,
            isPlayerRemote = false,
            nextEpisode = createEpisode(downloadUrl = null),
            warnOnMeteredNetwork = false,
        )

        assertNull(result)
    }

    @Test
    fun `should use UNMETERED constraint when warn on metered network is enabled`() {
        val result = buildPrefetchRequest(
            isFeatureEnabled = true,
            isPlayerRemote = false,
            nextEpisode = createEpisode(),
            warnOnMeteredNetwork = true,
        )

        assertEquals(NetworkType.UNMETERED, result?.networkConstraint)
    }

    @Test
    fun `should use CONNECTED constraint when warn on metered network is disabled`() {
        val result = buildPrefetchRequest(
            isFeatureEnabled = true,
            isPlayerRemote = false,
            nextEpisode = createEpisode(),
            warnOnMeteredNetwork = false,
        )

        assertEquals(NetworkType.CONNECTED, result?.networkConstraint)
    }

    @Test
    fun `should not prefetch on Wear OS`() {
        val result = buildPrefetchRequest(
            isFeatureEnabled = true,
            isPlayerRemote = false,
            nextEpisode = createEpisode(),
            warnOnMeteredNetwork = false,
            appPlatform = AppPlatform.WearOs,
        )

        assertNull(result)
    }

    @Test
    fun `should prefetch when player is null`() {
        val result = buildPrefetchRequest(
            isFeatureEnabled = true,
            isPlayerRemote = null,
            nextEpisode = createEpisode(),
            warnOnMeteredNetwork = false,
        )

        assertEquals("episode-uuid", result?.episodeUuid)
    }
}
