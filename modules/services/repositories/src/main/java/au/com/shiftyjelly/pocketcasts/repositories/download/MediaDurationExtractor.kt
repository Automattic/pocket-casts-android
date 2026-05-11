package au.com.shiftyjelly.pocketcasts.repositories.download

import android.media.MediaExtractor
import android.media.MediaFormat
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import java.io.File
import javax.inject.Inject

interface MediaDurationExtractor {
    fun extractDurationInSeconds(file: File): Double?
}

class MediaDurationExtractorImpl @Inject constructor() : MediaDurationExtractor {
    override fun extractDurationInSeconds(file: File): Double? {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(file.path)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                if (!format.containsKey(MediaFormat.KEY_DURATION)) {
                    continue
                }
                val durationUs = format.getLong(MediaFormat.KEY_DURATION)
                if (durationUs <= 0) {
                    continue
                }
                return durationUs / 1_000_000.0
            }
            null
        } catch (e: Exception) {
            LogBuffer.i(LogBuffer.TAG_DOWNLOAD, e, "Failed to extract media duration from ${file.path}")
            null
        } finally {
            extractor.release()
        }
    }
}
