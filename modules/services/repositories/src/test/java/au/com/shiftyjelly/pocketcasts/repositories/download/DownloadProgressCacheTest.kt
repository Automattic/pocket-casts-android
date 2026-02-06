package au.com.shiftyjelly.pocketcasts.repositories.download

import app.cash.turbine.test
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadProgressCacheTest {
    private val cache = DownloadProgressCache()

    @Test
    fun `update progress`() = runTest {
        cache.progressFlow("episode-id").test {
            assertNull(awaitItem())

            cache.updateProgress("episode-id", 30, 100)
            assertEquals(DownloadProgress(30, 100), awaitItem())

            cache.updateProgress("episode-id", 50, 100)
            assertEquals(DownloadProgress(50, 100), awaitItem())

            cancel()
        }
    }

    @Test
    fun `track progress separately for different episodes`() = runTest {
        cache.updateProgress("episode-id-1", 11, 200)
        cache.updateProgress("episode-id-2", 77, 100)

        assertEquals(DownloadProgress(11, 200), cache.progressFlow("episode-id-1").value)
        assertEquals(DownloadProgress(77, 100), cache.progressFlow("episode-id-2").value)
    }

    @Test
    fun `clear progress`() = runTest {
        cache.updateProgress("episode-id", 15, 100)

        cache.clearProgress("episode-id")

        assertNull(cache.progressFlow("episode-id").value)
    }
}
