package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpUtil
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.ContentMetadataMutations
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer

/**
 * Detects when a cache miss fetches a different build of the media (e.g. new dynamically inserted ads) than the
 * cache holds, by comparing the served file size with the cached length. On a mismatch it evicts the stale cache
 * and throws a [Cache.CacheException] so [CacheDataSource] retries straight from upstream.
 */
@OptIn(UnstableApi::class)
internal class CachedLengthValidatingDataSource(
    private val upstream: DataSource,
    private val cache: Cache,
    private val validateWhileReading: Boolean = true,
) : DataSource {
    private var mediaBuildChanged = false
    private var validatedKey: String? = null
    private var validatedLength = C.LENGTH_UNSET.toLong()

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        validatedKey = null
        val length = upstream.open(dataSpec)
        val key = dataSpec.key
        if (key == null || mediaBuildChanged || !FeatureFlag.isEnabled(Feature.VALIDATE_CACHED_MEDIA_LENGTH)) {
            return length
        }
        val servedLength = servedDocumentSize(dataSpec)
        if (servedLength == C.LENGTH_UNSET.toLong()) {
            return length
        }
        val cachedLength = ContentMetadata.getContentLength(cache.getContentMetadata(key))
        when (cachedLength) {
            C.LENGTH_UNSET.toLong() -> cache.storeContentLength(key, servedLength)

            servedLength -> Unit

            else -> {
                mediaBuildChanged = true
                upstream.close()
                cache.removeResource(key)
                cache.storeContentLength(key, servedLength)
                val message = "Media for '$key' changed length from $cachedLength to $servedLength bytes"
                LogBuffer.i(LogBuffer.TAG_PLAYBACK, message)
                throw MediaBuildChangedException(message)
            }
        }
        if (validateWhileReading) {
            validatedKey = key
            validatedLength = servedLength
        }
        return length
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val key = validatedKey
        if (key != null) {
            val cachedLength = ContentMetadata.getContentLength(cache.getContentMetadata(key))
            if (cachedLength != C.LENGTH_UNSET.toLong() && cachedLength != validatedLength) {
                mediaBuildChanged = true
                validatedKey = null
                val message = "Media for '$key' changed length from $validatedLength to $cachedLength bytes while reading"
                LogBuffer.i(LogBuffer.TAG_PLAYBACK, message)
                throw MediaBuildChangedException(message)
            }
        }
        return upstream.read(buffer, offset, length)
    }

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() {
        validatedKey = null
        upstream.close()
    }

    private fun servedDocumentSize(dataSpec: DataSpec): Long {
        val contentRange = responseHeader("Content-Range")
        if (contentRange != null) {
            return HttpUtil.getDocumentSize(contentRange)
        }
        val isWholeDocument = dataSpec.position == 0L && dataSpec.length == C.LENGTH_UNSET.toLong()
        if (!isWholeDocument) {
            return C.LENGTH_UNSET.toLong()
        }
        return responseHeader("Content-Length")?.toLongOrNull() ?: C.LENGTH_UNSET.toLong()
    }

    private fun responseHeader(name: String): String? {
        return upstream.responseHeaders.entries
            .firstOrNull { (key, _) -> key.equals(name, ignoreCase = true) }
            ?.value
            ?.firstOrNull()
    }

    class Factory(
        private val upstreamFactory: DataSource.Factory,
        private val cache: Cache,
        private val validateWhileReading: Boolean = true,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            return CachedLengthValidatingDataSource(upstreamFactory.createDataSource(), cache, validateWhileReading)
        }
    }
}

@OptIn(UnstableApi::class)
internal class MediaBuildChangedException(message: String) : Cache.CacheException(message)

internal fun Cache.discardMixedBuild(key: String, error: Throwable) {
    if (generateSequence(error) { it.cause }.none { it is MediaBuildChangedException }) {
        return
    }
    val length = ContentMetadata.getContentLength(getContentMetadata(key))
    removeResource(key)
    if (length != C.LENGTH_UNSET.toLong()) {
        storeContentLength(key, length)
    }
}

private fun Cache.storeContentLength(key: String, length: Long) {
    val mutations = ContentMetadataMutations()
    ContentMetadataMutations.setContentLength(mutations, length)
    applyContentMetadataMutations(key, mutations)
}
