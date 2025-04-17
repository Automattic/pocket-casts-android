package au.com.shiftyjelly.pocketcasts.repositories.notification

import jakarta.inject.Inject
import java.time.Clock
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NotificationDelayCalculator @Inject constructor(
    private val clock: Clock,
) {
    /**
     * Calculates the delay until the notification should be sent.
     *
     * @param type The notification type containing dayOffset
     * @return Delay in milliseconds until the target 10 AM
     */
    fun calculateDelayForOnboardingNotification(
        type: OnboardingNotificationType,
    ): Long {
        val now = clock.instant().toEpochMilli()
        val next10AM = calculateBase10AM(now)
        return next10AM + TimeUnit.DAYS.toMillis(type.dayOffset.toLong()) - now
    }

    private fun calculateBase10AM(currentTimeMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= currentTimeMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }

    /**
     * Calculates the delay until the re-engagement check worker should be triggered.
     * The worker is set to run daily at 4 PM.
     *
     * @return Delay in milliseconds until the next 4 PM.
     */
    fun calculateDelayForReEngagementCheck(): Long {
        val now = clock.instant().toEpochMilli()
        val next4PM = calculateBase4PM(now)
        return next4PM - now
    }

    private fun calculateBase4PM(currentTimeMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
            set(Calendar.HOUR_OF_DAY, 16)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= currentTimeMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }
}
