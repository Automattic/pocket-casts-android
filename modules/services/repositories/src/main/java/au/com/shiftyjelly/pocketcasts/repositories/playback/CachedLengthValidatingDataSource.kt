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

@OptIn(UnstableApi::class)
internal class CachedLengthValidatingDataSource(
    private val upstream: DataSource,
    private val cache: Cache,
) : DataSource {
    private var mediaBuildChanged = false

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
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
            C.LENGTH_UNSET.toLong() -> storeLength(key, servedLength)

            servedLength -> Unit

            else -> {
                mediaBuildChanged = true
                upstream.close()
                cache.removeResource(key)
                storeLength(key, servedLength)
                val message = "Media for '$key' changed length from $cachedLength to $servedLength bytes"
                LogBuffer.i(LogBuffer.TAG_PLAYBACK, message)
                throw MediaBuildChangedException(message)
            }
        }
        return length
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return upstream.read(buffer, offset, length)
    }

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() {
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

    private fun storeLength(key: String, length: Long) {
        val mutations = ContentMetadataMutations()
        ContentMetadataMutations.setContentLength(mutations, length)
        cache.applyContentMetadataMutations(key, mutations)
    }

    class Factory(
        private val upstreamFactory: DataSource.Factory,
        private val cache: Cache,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            return CachedLengthValidatingDataSource(upstreamFactory.createDataSource(), cache)
        }
    }
}

@OptIn(UnstableApi::class)
internal class MediaBuildChangedException(message: String) : Cache.CacheException(message)
