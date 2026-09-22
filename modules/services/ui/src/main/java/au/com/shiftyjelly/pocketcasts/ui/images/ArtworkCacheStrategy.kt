package au.com.shiftyjelly.pocketcasts.ui.images

import coil3.annotation.ExperimentalCoilApi
import coil3.network.CacheStrategy
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.request.Options

/**
 * Coil caches 404, 410 and a few other error codes by default and then replays them from disk on
 * every later load, so a cover that failed once stays broken even after the server recovers. Error
 * responses are not stored, and any that a previous version already stored are re-requested.
 */
@OptIn(ExperimentalCoilApi::class)
internal class ArtworkCacheStrategy : CacheStrategy {

    override suspend fun read(
        cacheResponse: NetworkResponse,
        networkRequest: NetworkRequest,
        options: Options,
    ) = if (cacheResponse.code in HTTP_SUCCESS_RANGE) {
        CacheStrategy.ReadResult(cacheResponse)
    } else {
        CacheStrategy.ReadResult(networkRequest)
    }

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
