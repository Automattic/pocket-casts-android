package au.com.shiftyjelly.pocketcasts.ui.images

import coil3.annotation.ExperimentalCoilApi
import coil3.network.CacheStrategy
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.request.Options
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoilApi::class)
class ArtworkCacheStrategyTest {

    private val strategy = ArtworkCacheStrategy()
    private val request = NetworkRequest("https://static.pocketcasts.com/artwork.webp")
    private val options = mock<Options>()

    @Test
    fun `successful responses are cached`() = runTest {
        val response = NetworkResponse(code = 200)

        val result = strategy.write(null, request, response, options)

        assertSame(response, result.response)
    }

    @Test
    fun `responses coil would otherwise cache are not allowed to replace artwork`() = runTest {
        for (code in listOf(300, 301, 404, 405, 410, 414, 501)) {
            val result = strategy.write(NetworkResponse(code = 200), request, NetworkResponse(code = code), options)

            assertEquals("HTTP $code must not be written to the disk cache", CacheStrategy.WriteResult.DISABLED, result)
        }
    }

    @Test
    fun `server errors are not cached`() = runTest {
        val result = strategy.write(null, request, NetworkResponse(code = 500), options)

        assertEquals(CacheStrategy.WriteResult.DISABLED, result)
    }

    @Test
    fun `cached artwork is returned without revalidating`() = runTest {
        val cached = NetworkResponse(code = 200)

        val result = strategy.read(cached, request, options)

        assertSame(cached, result.response)
        assertEquals(null, result.request)
    }
}
