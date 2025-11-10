package au.com.shiftyjelly.pocketcasts.repositories.appreview

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import java.time.Clock
import javax.inject.Inject

class AppReviewExceptionHandler @Inject constructor(
    private val settings: Settings,
    private val clock: Clock,
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, e: Throwable) {
        runCatching {
            settings.appReviewCrashTimestamp.set(clock.instant(), updateModifiedAt = false, commit = true)
        }
    }
}
