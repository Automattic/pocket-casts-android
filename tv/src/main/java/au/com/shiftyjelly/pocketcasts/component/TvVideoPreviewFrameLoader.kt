package au.com.shiftyjelly.pocketcasts.component

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val FRAME_POSITION_PERCENT = 0.1
private const val FRAME_TARGET_WIDTH = 640
private const val FRAME_TARGET_HEIGHT = 360
private const val MAX_CACHED_FRAMES = 32

object TvVideoPreviewFrameLoader {
    private val cache = LruCache<String, Bitmap>(MAX_CACHED_FRAMES)

    suspend fun frameFor(videoUrl: String): Bitmap? {
        cache.get(videoUrl)?.let { return it }
        val frame = withContext(Dispatchers.IO) { extractFrame(videoUrl) } ?: return null
        cache.put(videoUrl, frame)
        return frame
    }

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
            null
        } finally {
            retriever.release()
        }
    }
}
