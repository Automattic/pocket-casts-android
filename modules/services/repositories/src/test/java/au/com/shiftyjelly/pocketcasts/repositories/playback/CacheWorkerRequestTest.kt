package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.work.NetworkType
import org.junit.Assert.assertEquals
import org.junit.Test

class CacheWorkerRequestTest {

    @Test
    fun `should apply an unmetered network constraint to the cache work request`() {
        val request = CacheWorker.buildCacheWorkRequest(
            url = "https://example.com/episode.mp3",
            episodeUuid = "episode-uuid",
            networkConstraint = NetworkType.UNMETERED,
            maxCacheBytes = MAX_CACHE_BYTES,
        )

        assertEquals(NetworkType.UNMETERED, request.requiredNetworkType())
    }

    @Test
    fun `should apply a connected network constraint to the cache work request`() {
        val request = CacheWorker.buildCacheWorkRequest(
            url = "https://example.com/episode.mp3",
            episodeUuid = "episode-uuid",
            networkConstraint = NetworkType.CONNECTED,
            maxCacheBytes = MAX_CACHE_BYTES,
        )

        assertEquals(NetworkType.CONNECTED, request.requiredNetworkType())
    }

    @Test
    fun `should carry the max cache bytes in the cache work request input data`() {
        val request = CacheWorker.buildCacheWorkRequest(
            url = "https://example.com/episode.mp3",
            episodeUuid = "episode-uuid",
            networkConstraint = NetworkType.UNMETERED,
            maxCacheBytes = MAX_CACHE_BYTES,
        )

        assertEquals(MAX_CACHE_BYTES, request.maxCacheBytes())
    }

    @Suppress("RestrictedApi")
    private fun androidx.work.OneTimeWorkRequest.requiredNetworkType() = workSpec.constraints.requiredNetworkType

    @Suppress("RestrictedApi")
    private fun androidx.work.OneTimeWorkRequest.maxCacheBytes() = workSpec.input.getLong("max_cache_bytes_key", -1L)

    private companion object {
        const val MAX_CACHE_BYTES = 500L * 1024 * 1024
    }
}
