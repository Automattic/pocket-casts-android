package au.com.shiftyjelly.pocketcasts.repositories.download

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadProgressCacheTest {
    private val cache = DownloadProgressCache()

    @Test
    fun `update progress`() = runTest {
        cache.progressFlow("episode-id").test {
            assertEquals(null, awaitItem())

            cache.updateProgress("episode-id", 0.3)
            assertEquals(0.3, awaitItem()!!, 0.001)

            cache.updateProgress("episode-id", 0.5)
            assertEquals(0.5, awaitItem()!!, 0.001)

            cancel()
        }
    }

    @Test
    fun `do not update progress for small increments`() = runTest {
        cache.progressFlow("episode-id").test {
            assertEquals(null, awaitItem())

            cache.updateProgress("episode-id", 0.001)
            assertEquals(0.0, awaitItem()!!, 0.001)

            cache.updateProgress("episode-id", 0.004)
            expectNoEvents()

            cache.updateProgress("episode-id", 0.005)
            assertEquals(0.01, awaitItem()!!, 0.001)

            cancel()
        }
    }

    @Test
    fun `track progress separately for different episodes`() = runTest {
        cache.updateProgress("episode-id-1", 0.5)
        cache.updateProgress("episode-id-2", 0.4)

        assertEquals(0.5, cache.progressFlow("episode-id-1").value!!, 0.001)
        assertEquals(0.4, cache.progressFlow("episode-id-2").value!!, 0.001)
    }

    @Test
    fun `coerce min progress to 0`() = runTest {
        cache.updateProgress("episode-id", -0.1)

        assertEquals(0.0, cache.progressFlow("episode-id").value!!, 0.001)
    }

    @Test
    fun `coerce max progress to 1`() = runTest {
        cache.updateProgress("episode-id", 1.1)

        assertEquals(1.0, cache.progressFlow("episode-id").value!!, 0.001)
    }

    @Test
    fun `clear progress`() = runTest {
        cache.updateProgress("episode-id", 0.5)
        cache.clearProgress("episode-id")

        assertEquals(null, cache.progressFlow("episode-id").value)
    }
}
