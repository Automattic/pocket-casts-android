package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import android.media.MediaDataSource
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec

/**
 * Adapts a Media3 [DataSource] to [MediaDataSource] so MediaExtractor reads go through the app's
 * HTTP stack. When [cachedLengthAt] is provided, reads follow the player's cache writes instead of
 * racing them over the network.
 */
@OptIn(UnstableApi::class)
internal class StreamingMediaDataSource(
    private val dataSourceFactory: DataSource.Factory,
    private val uri: Uri,
    private val cacheKey: String? = null,
    private val isActive: () -> Boolean = { true },
    private val cachedLengthAt: ((position: Long, length: Long) -> Long)? = null,
) : MediaDataSource() {
    private var dataSource: DataSource? = null
    private var position = -1L
    private var totalSize = C.LENGTH_UNSET.toLong()

    override fun getSize(): Long {
        if (totalSize == C.LENGTH_UNSET.toLong()) {
            openAt(0)
        }
        return totalSize
    }

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (size == 0) return 0
        if (cachedLengthAt != null) {
            var waited = 0L
            while (cachedLengthAt.invoke(position, size.toLong()) <= 0L && waited < FOLLOW_TIMEOUT_MS && isActive()) {
                Thread.sleep(POLL_MS)
                waited += POLL_MS
            }
        }
        if (!isActive()) return -1
        if (position != this.position || dataSource == null) {
            openAt(position)
        }
        val read = dataSource?.read(buffer, offset, size) ?: return -1
        if (read == C.RESULT_END_OF_INPUT) return -1
        this.position += read
        return read
    }

    override fun close() {
        closeSource()
        position = -1L
    }

    private fun openAt(position: Long) {
        closeSource()
        val source = dataSourceFactory.createDataSource()
        val opened = source.open(DataSpec.Builder().setUri(uri).setKey(cacheKey).setPosition(position).build())
        if (position == 0L && opened != C.LENGTH_UNSET.toLong()) {
            totalSize = opened
        }
        dataSource = source
        this.position = position
    }

    private fun closeSource() {
        runCatching { dataSource?.close() }
        dataSource = null
    }

    private companion object {
        private const val POLL_MS = 100L
        private const val FOLLOW_TIMEOUT_MS = 60_000L
    }
}
