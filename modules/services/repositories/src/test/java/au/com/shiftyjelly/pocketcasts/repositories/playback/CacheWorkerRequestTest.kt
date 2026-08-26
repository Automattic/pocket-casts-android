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
        )

        assertEquals(NetworkType.UNMETERED, request.requiredNetworkType())
    }

    @Test
    fun `should apply a connected network constraint to the cache work request`() {
        val request = CacheWorker.buildCacheWorkRequest(
            url = "https://example.com/episode.mp3",
            episodeUuid = "episode-uuid",
            networkConstraint = NetworkType.CONNECTED,
        )

        assertEquals(NetworkType.CONNECTED, request.requiredNetworkType())
    }

    @Suppress("RestrictedApi")
    private fun androidx.work.OneTimeWorkRequest.requiredNetworkType() = workSpec.constraints.requiredNetworkType
}
