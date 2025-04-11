package au.com.shiftyjelly.pocketcasts.repositories.notification

import java.util.Calendar
import java.util.concurrent.TimeUnit

class NotificationDelayCalculator {
    /**
     * Calculates the delay until the notification should be sent.
     *
     * @param type The notification type containing dayOffset
     * @param currentTimeMillis current time
     * @return Delay in milliseconds until the target 10 AM
     */
    fun calculateDelayForOnboardingNotification(
        type: OnboardingNotificationType,
        currentTimeMillis: Long = System.currentTimeMillis(),
    ): Long {
        val next10AM = calculateBase10AM(currentTimeMillis)
        return next10AM + TimeUnit.DAYS.toMillis(type.dayOffset.toLong()) - currentTimeMillis
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
}
