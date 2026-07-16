package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import android.media.MediaDataSource
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec

@OptIn(UnstableApi::class)
internal class CacheBackedMediaDataSource(
    private val dataSourceFactory: DataSource.Factory,
    private val uri: Uri,
    private val cacheKey: String,
    private val cachedLengthAt: (position: Long, length: Long) -> Long,
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
        var waited = 0L
        while (cachedLengthAt(position, size.toLong()) <= 0L && waited < FOLLOW_TIMEOUT_MS) {
            Thread.sleep(POLL_MS)
            waited += POLL_MS
        }
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
