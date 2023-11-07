package au.com.shiftyjelly.pocketcasts.models.to

import okhttp3.HttpUrl
import kotlin.math.roundToInt

data class Chapter(
    val title: String,
    var startTime: Int,
    var endTime: Int,
    val url: HttpUrl? = null,
    val imagePath: String? = null,
    val mimeType: String? = null,
    var index: Int = 0
) {

    val isImagePresent: Boolean
        get() = imagePath != null && imagePath.isNotBlank()

    val isValid: Boolean
        get() = startTime >= 0 && endTime > 0

    val duration: Int
        get() = endTime - startTime

    fun containsTime(time: Int): Boolean {
        return time >= startTime && time < endTime || time >= startTime && endTime <= 0
    }

    fun beforeTime(currentTimeMs: Int): Boolean {
        return endTime <= currentTimeMs && endTime != -1
    }

    fun remainingTime(playbackPositionMs: Int): String {
        val progress = calculateProgress(playbackPositionMs)
        val length = endTime - startTime
        val remaining = length * (1f - progress)
        val minutesRemaining = remaining / 1000f / 60f
        return if (minutesRemaining >= 1) {
            "${minutesRemaining.toInt()}m"
        } else {
            val secondsRemaining = remaining / 1000f
            "${secondsRemaining.toInt()}s"
        }
    }

    fun calculateProgress(playbackPositionMs: Int): Float {
        if (playbackPositionMs == 0 || playbackPositionMs < startTime || playbackPositionMs > endTime || duration <= 0) {
            return 0f
        }
        return (playbackPositionMs - startTime).toFloat() / duration.toFloat()
    }
}
