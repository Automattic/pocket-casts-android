package au.com.shiftyjelly.pocketcasts.models.to

import kotlin.math.roundToInt
import kotlin.time.Duration
import okhttp3.HttpUrl

data class Chapter(
    val title: String,
    val startTime: Duration,
    val endTime: Duration,
    val index: Int,
    val uiIndex: Int,
    val url: HttpUrl? = null,
    val imagePath: String? = null,
    val selected: Boolean = true,
) {

    val isImagePresent: Boolean
        get() = !imagePath.isNullOrBlank()

    val duration: Duration
        get() = endTime - startTime

    operator fun contains(duration: Duration): Boolean {
        return duration in startTime..<endTime || duration > startTime && endTime <= Duration.ZERO
    }

    fun remainingTime(
        playbackPosition: Duration,
        playbackSpeed: Double,
        adjustRemainingTimeDuration: Boolean,
    ): String {
        val progress = calculateProgress(playbackPosition)
        val baseDuration = duration * (1.0 - progress)
        val remaining = if (adjustRemainingTimeDuration) {
            baseDuration / playbackSpeed
        } else {
            baseDuration
        }
        return if (remaining.inWholeMilliseconds >= 59500) {
            "${(remaining.inWholeSeconds / 60.0).roundToInt()}m"
        } else {
            "${(remaining.inWholeMilliseconds / 1000.0).roundToInt()}s"
        }
    }

    fun calculateProgress(playbackPosition: Duration): Float = when {
        playbackPosition == Duration.ZERO || playbackPosition !in this -> 0f
        duration.inWholeMilliseconds == 0L -> 0f
        else -> (playbackPosition - startTime).inWholeMilliseconds.toFloat() / duration.inWholeMilliseconds
    }
}
