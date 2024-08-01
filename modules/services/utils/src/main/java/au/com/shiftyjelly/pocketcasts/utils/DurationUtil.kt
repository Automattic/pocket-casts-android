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
