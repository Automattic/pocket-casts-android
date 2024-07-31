package au.com.shiftyjelly.pocketcasts.utils

import java.util.Locale
import kotlin.time.Duration

fun Duration.toHhMmSs() = toComponents { hours, minutes, seconds, _ ->
    if (hours == 0L) {
        kotlin.String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    } else {
        kotlin.String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
    }
}
