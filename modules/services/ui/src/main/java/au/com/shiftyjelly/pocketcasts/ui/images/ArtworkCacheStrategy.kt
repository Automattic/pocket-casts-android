package au.com.shiftyjelly.pocketcasts.ui.images

import coil3.annotation.ExperimentalCoilApi
import coil3.network.CacheStrategy
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.request.Options

/**
 * Coil caches 404, 410 and other error responses by default, which replaces a good cover with an
 * error that is then replayed from disk on every later load. Only successful responses are stored,
 * so a failing artwork server can never evict artwork the app already has.
 */
@OptIn(ExperimentalCoilApi::class)
internal class ArtworkCacheStrategy : CacheStrategy {

    // Unconditional, so Coil never sends a validator and [write] can never see a 304. Adding
    // revalidation here obliges [write] to return the 304 with a null body, otherwise Coil re-runs
    // the request and then fails to decode the empty body.
    override suspend fun read(
        cacheResponse: NetworkResponse,
        networkRequest: NetworkRequest,
        options: Options,
    ) = CacheStrategy.ReadResult(cacheResponse)

    // Only reached for codes Coil obtained from the network; see [read] before widening this set.
    override suspend fun write(
        cacheResponse: NetworkResponse?,
        networkRequest: NetworkRequest,
        networkResponse: NetworkResponse,
        options: Options,
    ) = if (networkResponse.code in HTTP_SUCCESS_RANGE) {
        CacheStrategy.WriteResult(networkResponse)
    } else {
        CacheStrategy.WriteResult.DISABLED
    }

    private companion object {
        val HTTP_SUCCESS_RANGE = 200 until 300
    }
}
