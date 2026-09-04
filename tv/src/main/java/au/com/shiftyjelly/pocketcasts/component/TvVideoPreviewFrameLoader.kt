package au.com.shiftyjelly.pocketcasts.component

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val FRAME_POSITION_PERCENT = 0.1
private const val FRAME_TARGET_WIDTH = 640
private const val FRAME_TARGET_HEIGHT = 360
private const val MAX_CACHE_BYTES = 8 * 1024 * 1024
private const val MAX_FAILED_URLS = 128

object TvVideoPreviewFrameLoader {
    private val cache = object : LruCache<String, Bitmap>(MAX_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val failed = LruCache<String, Boolean>(MAX_FAILED_URLS)

    suspend fun frameFor(videoUrl: String): Bitmap? {
        cache.get(videoUrl)?.let { return it }
        if (failed.get(videoUrl) != null || isHlsUrl(videoUrl)) {
            failed.put(videoUrl, true)
            return null
        }
        val frame = withContext(Dispatchers.IO) { extractFrame(videoUrl) }
        if (frame == null) {
            failed.put(videoUrl, true)
            return null
        }
        cache.put(videoUrl, frame)
        return frame
    }

    private fun isHlsUrl(videoUrl: String) = videoUrl.substringBefore('?').endsWith(".m3u8", ignoreCase = true)

    private fun extractFrame(videoUrl: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoUrl, emptyMap())
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val positionUs = (durationMs * 1000 * FRAME_POSITION_PERCENT).toLong()
            retriever.getScaledFrameAtTime(
                positionUs,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                FRAME_TARGET_WIDTH,
                FRAME_TARGET_HEIGHT,
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to extract TV discover video preview frame")
            null
        } finally {
            retriever.release()
        }
    }
}
