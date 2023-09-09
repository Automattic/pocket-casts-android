package au.com.shiftyjelly.pocketcasts.models.to

import okhttp3.HttpUrl
import kotlin.math.roundToInt

data class Chapter(
    val title: String,
    val url: HttpUrl?,
    var startTime: Int,
    var endTime: Int,
    val imagePath: String?,
    val mimeType: String?,
    var played: Boolean = false,
    var index: Int = 0,
    var progress: Float = 0.0f
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

    fun remainingTime(): String {
        val length = endTime - startTime
        val remaining = length * (1f - progress)
        val minutesRemaining = remaining / 1000f / 60f
        if (minutesRemaining >= 1) {
            return "${minutesRemaining.roundToInt()}m"
        } else {
            val secondsRemaining = remaining / 1000f
            return "${secondsRemaining.roundToInt()}s"
        }
    }

    fun progressPercentage(): Int {
        return (progress * 100f).roundToInt()
    }
}
