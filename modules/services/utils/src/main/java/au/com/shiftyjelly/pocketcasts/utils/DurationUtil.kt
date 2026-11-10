package au.com.shiftyjelly.pocketcasts.utils

import java.util.Locale
import kotlin.time.Duration

fun Duration.toHhMmSs() = buildString {
    if (this@toHhMmSs < Duration.ZERO) {
        append('-')
    }
    val formattedDuration = absoluteValue.toComponents { hours, minutes, seconds, _ ->
        if (hours == 0L) {
            String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
        }
    }
    append(formattedDuration)
}

fun Duration.toSecondsWithSingleMilli() = toComponents { hours, minutes, seconds, nanoseconds ->
    var totalSeconds = hours * 3600 + minutes * 60 + seconds
    var milliseconds = (nanoseconds + 50_000_000) / 100_000_000
    // Rounding nanoseconds to one decimal digit can itself round up to 10
    // (e.g. 950ms+ rounds to "10"), which would otherwise be emitted
    // verbatim as a bogus two-digit fraction like "1.10". Carry that into
    // the whole-seconds part instead, same as a normal round-up.
    if (milliseconds == 10) {
        totalSeconds += 1
        milliseconds = 0
    }
    buildString {
        append(totalSeconds)
        if (milliseconds != 0) {
            append('.')
            append(milliseconds)
        }
    }
}
