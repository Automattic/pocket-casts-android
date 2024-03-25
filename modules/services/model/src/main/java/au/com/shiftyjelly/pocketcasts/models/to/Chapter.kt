package au.com.shiftyjelly.pocketcasts.models.to

import kotlin.math.roundToInt
import kotlin.time.Duration
import okhttp3.HttpUrl

data class Chapter(
    val title: String,
    val startTime: Duration,
    val endTime: Duration,
    val url: HttpUrl? = null,
    val imagePath: String? = null,
    val index: Int = 0,
    val selected: Boolean = true,
) {

    val isImagePresent: Boolean
        get() = !imagePath.isNullOrBlank()

    val duration: Duration
        get() = endTime - startTime

    operator fun contains(duration: Duration): Boolean {
        return duration in startTime..endTime || duration > startTime && endTime <= Duration.ZERO
    }

    fun remainingTime(playbackPosition: Duration): String {
        val progress = calculateProgress(playbackPosition)
        val remaining = duration * (1.0 - progress)
        return if (remaining.inWholeMilliseconds >= 59500) {
            "${(remaining.inWholeSeconds / 60.0).roundToInt()}m"
        } else {
            "${(remaining.inWholeMilliseconds / 1000.0).roundToInt()}s"
        }
    }

    fun calculateProgress(playbackPosition: Duration): Float = if (playbackPosition == Duration.ZERO || playbackPosition !in this) {
        0f
    } else {
        (playbackPosition - startTime).inWholeMilliseconds.toFloat() / duration.inWholeMilliseconds.toFloat()
    }
}
