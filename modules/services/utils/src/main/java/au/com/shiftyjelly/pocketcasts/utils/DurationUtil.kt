package au.com.shiftyjelly.pocketcasts.utils

import java.util.Locale
import kotlin.time.Duration

fun Duration.toHhMmSs() = toComponents { hours, minutes, seconds, _ ->
    if (hours == 0L) {
        String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
    }
}

fun Duration.toSecondsWithSingleMilli() = toComponents { hours, minutes, seconds, nanoseconds ->
    val totalSeconds = hours * 3600 + minutes * 60 + seconds
    val milliseconds = (nanoseconds + 50_000_000) / 100_000_000
    buildString {
        append(totalSeconds)
        if (milliseconds != 0) {
            append('.')
            append(milliseconds)
        }
    }
}
